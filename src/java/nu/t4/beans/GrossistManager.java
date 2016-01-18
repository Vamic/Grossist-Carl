/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nu.t4.beans;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.ListIterator;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.HttpHeaders;
import org.mindrot.BCrypt;

/**
 *
 * @author carlkonig
 */
@Stateless
public class GrossistManager {

    private final String DB_USER = "sigma";
    private final String DB_PASS = "sigma";
    private final String DB_URL = "jdbc:mysql://localhost/grossistcarl";

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        String url = "jdbc:mysql://localhost/grossist";
        return (Connection) DriverManager
                .getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public int authCheck(HttpHeaders headers) {
        try {
            String encodedAuth = headers.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0);
            encodedAuth = encodedAuth.substring(encodedAuth.indexOf(" ") + 1);

            byte[] decoded = Base64.getDecoder().decode(encodedAuth);
            String decodedAuth = new String(decoded);
            String username = decodedAuth.substring(0, decodedAuth.indexOf(":"));
            String password = decodedAuth.substring(decodedAuth.indexOf(":") + 1);
            Connection conn = getConnection();
            Statement stmt = (Statement) conn.createStatement();
            String sql = String.format("SELECT * FROM kund WHERE"
                    + " epost='%s'", username);
            ResultSet data = stmt.executeQuery(sql);
            data.next();
            if (BCrypt.checkpw(password, data.getString("lösenord"))) {
                return data.getInt("id");
            } else {
                System.out.println("Pass didn't match.");
                return -1;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return -1;
        }
    }

    public JsonArray getOrders(int user_id) {
        try {
            Connection conn = getConnection();
            Statement stmt = (Statement) conn.createStatement();
            String sql = String.format("SELECT * FROM beställning WHERE"
                    + " kund_id=%d", user_id);
            ResultSet data = stmt.executeQuery(sql);
            JsonArrayBuilder builder = Json.createArrayBuilder();
            while (data.next()) {
                int id = data.getInt("id");
                String status = data.getString("status");
                JsonObjectBuilder obuilder = Json.createObjectBuilder();
                builder.add(obuilder.add("id", id).add("status", status).build());
            }
            conn.close();
            return builder.build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public JsonArray getOrder(int user_id, int order_id) {
        try {
            Connection conn = getConnection();
            Statement stmt = (Statement) conn.createStatement();
            String sql = String.format("SELECT * FROM vara,beställning_vara "
                    + "WHERE vara.id = vara_id AND (SELECT kund_id FROM beställning "
                    + "WHERE id = %d) = %d", order_id, user_id);
            ResultSet data = stmt.executeQuery(sql);
            JsonArrayBuilder builder = Json.createArrayBuilder();
            while (data.next()) {
                int id = data.getInt("id");
                String name = data.getString("namn");
                String description = data.getString("beskrivning");
                int price = data.getInt("pris");
                int item_category_id = data.getInt("kategori_id");
                int amount = data.getInt("mängd");
                JsonObjectBuilder obuilder = Json.createObjectBuilder();
                builder.add(obuilder
                        .add("id", id)
                        .add("name", name)
                        .add("description", description)
                        .add("price", price)
                        .add("category_id", item_category_id)
                        .add("amount", amount)
                        .build());
            }
            conn.close();
            return builder.build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public boolean placeOrder(int user_id, JsonObject object) {
        try {
            Connection conn = getConnection();
            Statement stmt = (Statement) conn.createStatement();

            //Add the order
            String sql = String.format("INSERT INTO beställning "
                    + "VALUES(null,0,%d); ", user_id);
            stmt.addBatch(sql);

            //Add the items
            sql = "INSERT INTO beställning_vara VALUES";
            JsonArray items = object.getJsonArray("items");
            ListIterator iterator = items.listIterator();
            while (iterator.hasNext()) {
                JsonObject item = (JsonObject) iterator.next();
                int item_id = item.getInt("id");
                int amount = item.getInt("amount");
                sql += String.format("(LAST_INSERT_ID(),%d,%d),", item_id, amount);
            }
            sql = sql.substring(0, sql.length() - 1);
            sql += ";";
            System.out.println(sql);
            stmt.addBatch(sql);

            //Update the level
            sql = String.format("UPDATE kund SET premienivå_id = "
                    + "(SELECT id FROM premienivå WHERE krav <= "
                    + "(SELECT COALESCE(SUM(pris*mängd),0) FROM vara,beställning_vara "
                    + "WHERE vara.id = vara_id AND %d = "
                    + "(SELECT kund_id FROM beställning "
                    + "WHERE beställning.id = beställning_id)) "
                    + "ORDER BY id DESC LIMIT 1) "
                    + "WHERE id = %d", user_id, user_id);
            stmt.addBatch(sql);
            stmt.executeBatch();
            return true;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public JsonArray getItems() {
        try {
            Connection conn = getConnection();
            Statement stmt = (Statement) conn.createStatement();
            String sql = String.format("SELECT * FROM vara");
            ResultSet data = stmt.executeQuery(sql);
            JsonArrayBuilder builder = Json.createArrayBuilder();
            while (data.next()) {
                int id = data.getInt("id");
                String name = data.getString("namn");
                String description = data.getString("beskrivning");
                int price = data.getInt("pris");
                int item_category_id = data.getInt("kategori_id");
                JsonObjectBuilder obuilder = Json.createObjectBuilder();
                builder.add(obuilder
                        .add("id", id)
                        .add("name", name)
                        .add("description", description)
                        .add("price", price)
                        .add("category_id", item_category_id)
                        .build());
            }
            conn.close();
            return builder.build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public JsonArray getItemsCategory(int category_id) {
        try {
            Connection conn = getConnection();
            Statement stmt = (Statement) conn.createStatement();
            String sql = String.format("SELECT * FROM vara "
                    + "WHERE kategori_id = %d", category_id);
            ResultSet data = stmt.executeQuery(sql);
            JsonArrayBuilder builder = Json.createArrayBuilder();
            while (data.next()) {
                int id = data.getInt("id");
                String name = data.getString("namn");
                String description = data.getString("beskrivning");
                int price = data.getInt("pris");
                int item_category_id = data.getInt("kategori_id");
                JsonObjectBuilder obuilder = Json.createObjectBuilder();
                builder.add(obuilder
                        .add("id", id)
                        .add("name", name)
                        .add("description", description)
                        .add("price", price)
                        .add("category_id", item_category_id)
                        .build());
            }
            conn.close();
            return builder.build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public JsonArray getItemsSupplier(int supplier_id) {
        try {
            Connection conn = getConnection();
            Statement stmt = (Statement) conn.createStatement();
            String sql = String.format("SELECT * FROM vara "
                    + "WHERE id IN (SELECT vara_id FROM vara_leverantör "
                    + "WHERE leverantör_id = %d)", supplier_id);
            ResultSet data = stmt.executeQuery(sql);
            JsonArrayBuilder builder = Json.createArrayBuilder();
            while (data.next()) {
                int id = data.getInt("id");
                String name = data.getString("namn");
                String description = data.getString("beskrivning");
                int price = data.getInt("pris");
                int item_category_id = data.getInt("kategori_id");
                JsonObjectBuilder obuilder = Json.createObjectBuilder();
                builder.add(obuilder
                        .add("id", id)
                        .add("name", name)
                        .add("description", description)
                        .add("price", price)
                        .add("category_id", item_category_id)
                        .build());
            }
            conn.close();
            return builder.build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public JsonArray getItemsCategoryCheapest(int category_id) {
        try {
            Connection conn = getConnection();
            Statement stmt = (Statement) conn.createStatement();
            String sql = String.format("SELECT * FROM vara "
                    + "WHERE kategori_id = %d "
                    + "ORDER BY pris LIMIT 10", category_id);
            ResultSet data = stmt.executeQuery(sql);
            JsonArrayBuilder builder = Json.createArrayBuilder();
            while (data.next()) {
                int id = data.getInt("id");
                String name = data.getString("namn");
                String description = data.getString("beskrivning");
                int price = data.getInt("pris");
                int item_category_id = data.getInt("kategori_id");
                JsonObjectBuilder obuilder = Json.createObjectBuilder();
                builder.add(obuilder
                        .add("id", id)
                        .add("name", name)
                        .add("description", description)
                        .add("price", price)
                        .add("category_id", item_category_id)
                        .build());
            }
            conn.close();
            return builder.build();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
}
