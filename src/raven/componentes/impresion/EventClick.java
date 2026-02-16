package raven.componentes.impresion;

import java.awt.Component;

public interface EventClick {
    // Métodos para productos
    void itemClick(DataSearch data);
    void itemRemove(Component com, DataSearch data);
    
    
}