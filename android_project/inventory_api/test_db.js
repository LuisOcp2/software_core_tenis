const mysql = require('mysql2/promise');
require('dotenv').config();

async function testConnection() {
    console.log(`Connecting to ${process.env.DB_HOST} with user ${process.env.DB_USER}...`);
    try {
        const connection = await mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASS,
            database: process.env.DB_NAME,
            timezone: '-05:00'
        });
        console.log('Successfully connected!');
        await connection.end();
    } catch (err) {
        console.error('Connection failed:', err.message);
        console.error('Error code:', err.code);
    }
}

testConnection();
