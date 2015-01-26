package org.safehaus.subutai.core.shiro.ui.tabs;


import com.vaadin.annotations.AutoGenerated;
import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;


/**
 * Created by talas on 1/26/15.
 */
public class PermissionsTab extends CustomComponent
{
    public PermissionsTab()
    {
        editableHeights();
        // TODO add user code here
    }


    @AutoGenerated
    void editableHeights()
    {
        VerticalLayout layout = new VerticalLayout();

        // BEGIN-EXAMPLE: component.table.editable.editableheights
        // Table with some typical data types
        final Table table = new Table( "Edible Table" );
        table.addContainerProperty( "Name", String.class, null );
        table.setDescription( "Permissions Caption" );
        table.setImmediate( true );

        // Some example data
        //TODO Retrieve permissions from database
        Object people[] = {
                "CRUD permissions", "Read permissions", "Update permissions"
        };

        // Insert the data
        for ( int i = 0; i < people.length; i++ )
        {
            Object roleCaption = people[i];
            Object obj[] = {
                    roleCaption
            };
            table.addItem( obj, i );
        }
        table.setPageLength( table.size() );

        // Set a custom field factory that overrides the default factory
        table.setEditable( true );

        table.addStyleName( "editableexample" );

        // Allow switching to non-editable mode
        final CheckBox editable = new CheckBox( "Table is editable", true );
        editable.addValueChangeListener( new Property.ValueChangeListener()
        {
            private static final long serialVersionUID = 6291942958587745232L;


            public void valueChange( Property.ValueChangeEvent event )
            {
                table.setEditable( editable.getValue() );
            }
        } );

        editable.setImmediate( true );
        // END-EXAMPLE: component.table.editable.editableheights

        final TextField newPermission = new TextField( "Input new permission name" );
        Button savePermission = new Button( "Save Permission" );
        savePermission.addClickListener( new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent event )
            {
                table.addItem( new Object[] { newPermission.getValue() } );
            }
        } );

        layout.addComponent( newPermission );
        layout.addComponent( savePermission );
        layout.addComponent( editable );
        layout.addComponent( table );

        setCompositionRoot( layout );
    }
}
