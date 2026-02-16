package raven.componentes.icon;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo class for JIconButton component.
 * This provides NetBeans with information about the properties and editors for the component.
 */
public class JIconButtonBeanInfo extends SimpleBeanInfo {
    
    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor descriptor = new BeanDescriptor(JIconButton.class);
        descriptor.setDisplayName("Icon Button");
        descriptor.setShortDescription("A button with FontAwesome icon");
        return descriptor;
    }
    
    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor iconType = new PropertyDescriptor("iconType", JIconButton.class);
            iconType.setDisplayName("Icon Type");
            iconType.setShortDescription("The FontAwesome icon type");
            iconType.setPropertyEditorClass(JIconButton.FontAwesomePropertyEditor.class);
            
            PropertyDescriptor svgIconPath = new PropertyDescriptor("svgIconPath", JIconButton.class);
            svgIconPath.setDisplayName("SVG Icon Path");
            svgIconPath.setShortDescription("The resource path to the SVG icon (e.g., /raven/icon/icons/imprimir.svg)");
            
            PropertyDescriptor iconSize = new PropertyDescriptor("iconSize", JIconButton.class);
            iconSize.setDisplayName("Icon Size");
            iconSize.setShortDescription("The size of the icon in pixels");
            
            PropertyDescriptor iconColor = new PropertyDescriptor("iconColor", JIconButton.class);
            iconColor.setDisplayName("Icon Color");
            iconColor.setShortDescription("The color of the icon (only applies to FontAwesome icons)");
            
            PropertyDescriptor background = new PropertyDescriptor("background", JIconButton.class);
            background.setDisplayName("Background Color");
            background.setShortDescription("The background color of the button");
            
            PropertyDescriptor text = new PropertyDescriptor("text", JIconButton.class);
            text.setDisplayName("Button Text");
            text.setShortDescription("The text to display on the button");
            
            return new PropertyDescriptor[]{
                iconType,
                svgIconPath,
                iconSize,
                iconColor,
                background,
                text
            };
            
        } catch (IntrospectionException e) {
            return null;
        }
    }
}