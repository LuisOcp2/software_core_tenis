/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package raven.controlador.principal;

/**
 *
 * @author CrisDEV
 * Configuracion del programa
 */

public class AppConfig {
    
    public final static String name = "GLOBAL TENNIS"; //nombre del programa
    public final static String logo = "/raven/icon/svg/logo.svg"; //logo del programa
    public final static String logo2 = "/raven/icon/svg/xtreme.svg"; //logo del programa
    public final static String logopng = "/raven/icon/logo.png"; //logo del programa en png
    public final static String logoico = "/raven/icon/logo.ico"; //logo del programa en png
      
    // --- NUEVO PARA ACTUALIZACIONES ---
     public final static String APP_VERSION = "1.0.31";
     public final static String UPDATE_JSON_URL ="https://software.jsglobalsolution.com/updates/zapatos_version.json";
     
     // --- DATABASE CONFIGURATION ---
     public final static int DB_CONNECT_TIMEOUT = 15000;
     public final static int DB_SOCKET_TIMEOUT = 60000;
     public final static int DB_MAX_TOTAL = 8;
     public final static int DB_MAX_IDLE = 4;
     public final static int DB_MIN_IDLE = 2;
     public final static int DB_MAX_WAIT_MILLIS = 15000;
}
