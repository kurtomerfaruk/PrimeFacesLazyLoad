package com.kurtomerfaruk.primefaceslazyload.bean;

import com.kurtomerfaruk.primefaceslazyload.session.AbstractFacade;
import com.kurtomerfaruk.primefaceslazyload.bean.util.JsfUtil;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;

import java.util.ResourceBundle;
import javax.ejb.EJBException;
import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

/**
 * Represents an abstract shell of to be used as JSF Controller to be used in
 * AJAX-enabled applications. No outcomes will be generated from its methods
 * since handling is designed to be done inside one page.
 */
public abstract class AbstractController<T> implements Serializable {

    @Inject
    private AbstractFacade<T> ejbFacade;
    private Class<T> itemClass;
    private T selected;
    private Collection<T> items;
    private LazyDataModel<T> liste;

    private enum PersistAction {

        CREATE,
        DELETE,
        UPDATE
    }

    public AbstractController() {
    }

    public AbstractController(Class<T> itemClass) {
        this.itemClass = itemClass;
    }

    /**
     * Retrieve the currently selected item
     *
     * @return
     */
    public T getSelected() {
        return selected;
    }

    /**
     * Pass in the currently selected item
     *
     * @param selected
     */
    public void setSelected(T selected) {
        this.selected = selected;
    }

    protected void setEmbeddableKeys() {
        // Nothing to do if entity does not have any embeddable key.
    }

    ;

    protected void initializeEmbeddableKey() {
        // Nothing to do if entity does not have any embeddable key.
    }

    /**
     * Returns all items as a Collection object
     *
     * @return
     */
    public Collection<T> getItems() {
        if (items == null) {
            items = this.ejbFacade.findAll();
        }
        System.out.println("itemsSize:"+items.size());
        return items;
    }

    /**
     * Pass in collection of items
     *
     * @param items
     */
    public void setItems(Collection<T> items) {
        this.items = items;
    }

    public LazyDataModel<T> getListe() {
        if (liste == null) {
            liste = new LazyDataModel<T>() {
                @Override
                public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
                    List<T> result = ejbFacade.loadLazy(first, pageSize, sortField, sortOrder, filters);
                    liste.setRowCount(ejbFacade.count(filters));
                    System.out.println("listeSize:"+result.size());
                    return result;
                }
            };
        }
        
        return liste;
    }

    public void setListe(LazyDataModel<T> liste) {
        this.liste = liste;
    }
    
    

    /**
     * Apply changes to an existing item to the data layer.
     *
     * @param event
     */
    public void save(ActionEvent event) {
        String msg = ResourceBundle.getBundle("/MyBundle").getString(itemClass.getSimpleName() + "Updated");
        persist(PersistAction.UPDATE, msg);
    }

    /**
     * Store a new item in the data layer.
     *
     * @param event
     */
    public void saveNew(ActionEvent event) {
        String msg = ResourceBundle.getBundle("/MyBundle").getString(itemClass.getSimpleName() + "Created");
        persist(PersistAction.CREATE, msg);
        if (!isValidationFailed()) {
            items = null; // Invalidate list of items to trigger re-query.
        }
    }

    /**
     * Remove an existing item from the data layer.
     *
     * @param event
     */
    public void delete(ActionEvent event) {
        String msg = ResourceBundle.getBundle("/MyBundle").getString(itemClass.getSimpleName() + "Deleted");
        persist(PersistAction.DELETE, msg);
        if (!isValidationFailed()) {
            selected = null; // Remove selection
            items = null; // Invalidate list of items to trigger re-query.
        }
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            this.setEmbeddableKeys();
            try {
                if (persistAction != PersistAction.DELETE) {
                    this.ejbFacade.edit(selected);
                } else {
                    this.ejbFacade.remove(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = JsfUtil.getRootCause(ex.getCause());
                if (cause != null) {
                    if (cause instanceof ConstraintViolationException) {
                        ConstraintViolationException excp = (ConstraintViolationException) cause;
                        for (ConstraintViolation s : excp.getConstraintViolations()) {
                            JsfUtil.addErrorMessage(s.getMessage());
                        }
                    } else {
                        msg = cause.getLocalizedMessage();
                        if (msg.length() > 0) {
                            JsfUtil.addErrorMessage(msg);
                        } else {
                            JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/MyBundle").getString("PersistenceErrorOccured"));
            }
        }
    }

    /**
     * Creates a new instance of an underlying entity and assigns it to Selected
     * property.
     *
     * @return
     */
    public T prepareCreate(ActionEvent event) {
        T newItem;
        try {
            newItem = itemClass.newInstance();
            this.selected = newItem;
            initializeEmbeddableKey();
            return newItem;
        } catch (InstantiationException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Inform the user interface whether any validation error exist on a page.
     *
     * @return
     */
    public boolean isValidationFailed() {
        return JsfUtil.isValidationFailed();
    }

    /**
     * Retrieve all messages as a String to be displayed on the page.
     *
     * @param clientComponent
     * @param defaultMessage
     * @return
     */
    public String getComponentMessages(String clientComponent, String defaultMessage) {
        return JsfUtil.getComponentMessages(clientComponent, defaultMessage);
    }

    @PostConstruct
    public void initParams() {
        Object paramItems = FacesContext.getCurrentInstance().getExternalContext().getRequestMap().get(itemClass.getSimpleName() + "_items");
        if (paramItems != null) {
            this.items = (Collection<T>) paramItems;
        }
    }

}
