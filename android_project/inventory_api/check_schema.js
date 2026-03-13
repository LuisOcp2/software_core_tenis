const mysql = require('mysql2/promise');
require('dotenv').config();

async function checkSchema() {
    try {
        const connection = await mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASS,
            database: process.env.DB_NAME,
            timezone: '-05:00'
        });

        console.log('--- Tables ---');
        const [tables] = await connection.query('SHOW TABLES');
        console.log(tables.map(t => Object.values(t)[0]));

        console.log('\n--- Describe usuarios ---');
        try {
            const [columns] = await connection.query('DESCRIBE usuarios');
            console.log(columns.map(c => c.Field));
        } catch (e) {
            console.log('Error describing usuarios:', e.message);
        }

        await connection.end();
    } catch (err) {
        console.error(err);
    }
}

checkSchema();
