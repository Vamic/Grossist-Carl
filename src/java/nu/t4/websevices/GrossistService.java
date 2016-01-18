/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nu.t4.websevices;

import java.io.StringReader;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import nu.t4.beans.GrossistManager;

/**
 *
 * @author carlkonig
 */
@Path("grossist")
public class GrossistService {

    @EJB
    GrossistManager manager;

    @POST
    @Path("/login")
    public Response authCheck(@Context HttpHeaders headers) {
        int user_id = manager.authCheck(headers);
        if (user_id >= 0) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/orders")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrders(@Context HttpHeaders headers) {
        int user_id = manager.authCheck(headers);
        if (user_id >= 0) {
            JsonArray data = manager.getOrders(user_id);
            if (data == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                return Response.ok(data).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/order/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrder(@Context HttpHeaders headers, @PathParam("id") int order_id) {
        int user_id = manager.authCheck(headers);
        if (user_id >= 0) {
            JsonArray data = manager.getOrder(user_id, order_id);
            if (data == null) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                return Response.ok(data).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @PUT
    @Path("/order")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response placeOrder(@Context HttpHeaders headers, String body) {
        int user_id = manager.authCheck(headers);
        if (user_id >= 0) {
            JsonReader reader = Json.createReader(new StringReader(body));
            JsonObject object = reader.readObject();
            reader.close();

            if (!manager.placeOrder(user_id, object)) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else {
                return Response.status(Response.Status.CREATED).build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @GET
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItems() {
        JsonArray data = manager.getItems();
        if (data == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } else {
            return Response.ok(data).build();
        }
    }
    
    @GET
    @Path("/items/category/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemsCategory(@PathParam("id") int category_id) {
        JsonArray data = manager.getItemsCategory(category_id);
        if (data == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } else {
            return Response.ok(data).build();
        }
    }
    
    @GET
    @Path("/items/supplier/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemsSupplier(@PathParam("id") int supplier_id) {
        JsonArray data = manager.getItemsSupplier(supplier_id);
        if (data == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } else {
            return Response.ok(data).build();
        }
    }
    
    @GET
    @Path("/items/category/{id}/cheapest")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemsCategoryCheapest(@PathParam("id") int category_id) {
        JsonArray data = manager.getItemsCategoryCheapest(category_id);
        if (data == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } else {
            return Response.ok(data).build();
        }
    }
    
}
