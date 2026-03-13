const express = require('express');
const mysql = require('mysql2/promise');
const cors = require('cors');
require('dotenv').config();
const bcrypt = require('bcryptjs');

const app = express();
app.use(cors());
app.use(express.json());

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    database: process.env.DB_NAME,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    timezone: '-05:00', // America/Bogota
    ssl: false,
    allowPublicKeyRetrieval: true,
    connectTimeout: 15000
});

// Test DB Connection
app.get('/api/test', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT 1 as result');
        res.json({ status: 'ok', message: 'Database Connected', result: rows[0] });
    } catch (err) {
        console.error(err);
        res.status(500).json({ status: 'error', message: err.message });
    }
});

// Login (Simple validation against 'usuarios' table)
// Based on typical schema, guessing column names. Will adjust if needed.
app.post('/api/login', async (req, res) => {
    const { username, password } = req.body;
    try {
        // 1. Fetch user by username (active users only)
        const [rows] = await pool.query(
            'SELECT id_usuario, username, password, nombre, rol, ubicacion, id_bodega FROM usuarios WHERE username = ? AND activo = 1',
            [username]
        );

        if (rows.length > 0) {
            const user = rows[0];
            const dbPassword = user.password;

            let passwordMatch = false;

            // 2. Check password
            // Check if it looks like a BCrypt hash
            if (dbPassword && dbPassword.length === 60 && (dbPassword.startsWith('$2a$') || dbPassword.startsWith('$2b$') || dbPassword.startsWith('$2y$'))) {
                passwordMatch = await bcrypt.compare(password, dbPassword);
            } else {
                // Legacy plain text check
                passwordMatch = (password === dbPassword);
            }

            if (passwordMatch) {
                // Remove password from response
                delete user.password;
                res.json({ success: true, user: user });
            } else {
                res.status(401).json({ success: false, message: 'Invalid credentials' });
            }
        } else {
            res.status(401).json({ success: false, message: 'Invalid credentials' });
        }
    } catch (err) {
        console.error(err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// Get Active Inventory Counts
app.get('/api/conteos', async (req, res) => {
    try {
        // Based on ConteoInventarioDAO.java: 
        // SELECT * FROM conteos_inventario WHERE estado != 'completado' ORDER BY fecha_programada DESC
        const [rows] = await pool.query(
            "SELECT c.id_conteo, c.nombre, c.fecha_programada, c.tipo, c.estado FROM conteos_inventario c WHERE c.estado != 'completado' ORDER BY c.fecha_programada DESC"
        );
        res.json(rows);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: err.message });
    }
});

// Get Count Details
app.get('/api/conteos/:id/detalles', async (req, res) => {
    const { id } = req.params;
    try {
        // Based on DAO: 
        // SELECT d.*, p.nombre as producto, pv.ean as codigo_barras 
        // FROM detalles_conteo_inventario d 
        // JOIN productos p ON d.id_producto = p.id_producto ...
        // Simplified query for mobile:
        const sql = `
            SELECT 
                d.id_detalle_conteo, 
                d.id_conteo,
                d.stock_contado,
                d.estado,
                p.nombre as nombre_producto,
                pv.ean as codigo,
                c.nombre as color,
                t.numero as talla
            FROM detalles_conteo_inventario d
            JOIN productos p ON d.id_producto = p.id_producto
            LEFT JOIN producto_variantes pv ON d.id_variante = pv.id_variante
            LEFT JOIN colores c ON pv.id_color = c.id_color
            LEFT JOIN tallas t ON pv.id_talla = t.id_talla
            WHERE d.id_conteo = ?
        `;
        const [rows] = await pool.query(sql, [id]);
        res.json(rows);
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: err.message });
    }
});

// Update Count for a specific detail (Search by Barcode capability planned in App logic)
// But endpoints should probably support updating by ID or finding by Barcode + ConteoID
app.post('/api/conteos/contar', async (req, res) => {
    const { id_detalle, cantidad, id_usuario } = req.body;

    console.log(`[CONTEO] Request received: DetailID=${id_detalle}, Qty=${cantidad}, UserID=${id_usuario}`);

    // Updates stock_contado and sets state to 'contado'
    // Also updates difference. logic from DAO: diferencia = stock_contado - stock_sistema
    // We can do this in SQL or fetch first. SQL is faster.

    try {
        const sql = `
            UPDATE detalles_conteo_inventario 
            SET 
                stock_contado = ?,
                diferencia = ? - stock_sistema,
                id_usuario_contador = ?,
                estado = 'contado',
                fecha_conteo = NOW()
            WHERE id_detalle_conteo = ?
        `;

        const [result] = await pool.query(sql, [cantidad, cantidad, id_usuario, id_detalle]);

        if (result.affectedRows > 0) {
            console.log(`[CONTEO] Success: DetailID=${id_detalle} updated. Rows affected: ${result.affectedRows}`);
            res.json({ success: true, message: 'Stock updated' });
        } else {
            console.warn(`[CONTEO] Warning: DetailID=${id_detalle} not found or not updated.`);
            res.status(404).json({ success: false, message: 'Detail not found' });
        }
    } catch (err) {
        console.error(`[CONTEO] Error updating DetailID=${id_detalle}:`, err.message);
        res.status(500).json({ success: false, error: err.message });
    }
});

// Find detail by barcode for a specific count
app.post('/api/conteos/buscar', async (req, res) => {
    const { id_conteo, codigo } = req.body;
    try {
        const { id_conteo, codigo } = req.body; // 'codigo' can be name or barcode
        const searchTerm = `%${codigo}%`;

        // Search by EAN (exact) OR Name (like)
        // Priority to exact EAN match if possible, but for simplicity we check both
        const sql = `
            SELECT 
                d.id_detalle_conteo, 
                d.stock_contado,
                p.nombre as nombre_producto,
                pv.ean as codigo,
                c.nombre as color,
                t.numero as talla
            FROM detalles_conteo_inventario d
            JOIN productos p ON d.id_producto = p.id_producto
            LEFT JOIN producto_variantes pv ON d.id_variante = pv.id_variante
            LEFT JOIN colores c ON pv.id_color = c.id_color
            LEFT JOIN tallas t ON pv.id_talla = t.id_talla
            WHERE d.id_conteo = ? 
            AND (pv.ean = ? OR p.nombre LIKE ?)
            LIMIT 1
        `;

        const [rows] = await pool.query(sql, [id_conteo, codigo, searchTerm]);

        if (rows.length > 0) {
            res.json({ success: true, data: rows[0] });
        } else {
            // Try searching strict name match in Java logic? 
            // For now, simple LIKE is good for mobile.
            res.json({ success: false, message: 'Producto no encontrado en este conteo' });
        }
    } catch (err) {
        console.error('[BUSCAR] Error:', err.message);
        res.status(500).json({ error: err.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Inventory API running on port ${PORT}`);
});
