package Contexts.Ticket.Infrastructure.MongoDB;

import Contexts.Product.Domain.*;
import Contexts.Ticket.Domain.Ticket;
import Contexts.Ticket.Domain.TicketRepository;
import FlowerStore.FlowerStore;
import Infrastructure.Connections.MongoDBConnection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.*;

public class TicketRepositoryMongoDB implements TicketRepository {
    private MongoCollection<Document> ticketCollection;
    private FlowerStore flowerStore;

    public TicketRepositoryMongoDB(MongoDBConnection mongoDBConnection, FlowerStore flowerStore) {
        this.ticketCollection = mongoDBConnection.mongoDatabase.getCollection("tickets");
        this.flowerStore = flowerStore;
    }

    public int getNextTicketId() {
        Document query = new Document("_id", "ticketID");
        Document update = new Document("$inc", new Document("sequence", 1));
        Document result = ticketCollection.findOneAndUpdate(query, update);

        if (result == null) {
            // Si no hay un documento existente para el contador, lo creamos
            ticketCollection.insertOne(new Document("_id", "ticketID").append("sequence", 1));
            return 1;
        } else {
            return result.getInteger("sequence");
        }
    }
    @Override
    public Ticket getLastTicket() {
        Document document = ticketCollection.find().sort(new Document("ticketID", -1 )).first();

        if (document == null) {
            System.out.println("Not found last ticket");
            return null;
        } else {
            return documentToTicket(document);
        }
    }


    public int nextTicketID() {

            Ticket lastTicket = getLastTicket();
            if (lastTicket == null) {
                return 1;
            } else {
                return lastTicket.getTicketID() + 1;
            }
    }

    private Ticket documentToTicket(Document document) {
        int ticketID = document.getInteger("ticketID");
        Date date = document.getDate("date");
        double totalPrice = document.getDouble("totalPrice");

        Map<Product, Integer> products = new HashMap<>();
        List<Document> productsInfo = (List<Document>) document.get("products");
        for (Document productInfo : productsInfo) {
            String name = productInfo.getString("Name");
            String type = productInfo.getString("Type");
            String features = productInfo.getString("Features");
            int quantity = productInfo.getInteger("Quantity");
            double price = productInfo.getDouble("Price");

            Product product;
            if (type.equals(ProductType.FLOWER.toString())) {
                product = new Flower<>(name, quantity, price, features);
            } else if (type.equals(ProductType.DECORATION.toString())) {
                product = new Decoration<>(name, quantity, price, features);
            } else if (type.equals(ProductType.TREE.toString())) {
                product = new Tree<>(name, quantity, price, Double.parseDouble(features));
            } else {
                throw new IllegalArgumentException("Invalid product type : " + type);
            }

            products.put(product, quantity);
        }

        return new Ticket(ticketID, date, products, totalPrice);
    }

    @Override
    public void newTicket(Ticket ticket) {

        List<Document> productList = new ArrayList<>();
        Map<Product, Integer> ticketInfo = ticket.getProducts();

        for (Map.Entry<Product, Integer> entry : ticketInfo.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();

            productList.add(new Document("Name", product.getName())
                    .append("Type", product.getType().toString())
                    .append("Features", product.getAttributes().toString())
                    .append("Quantity", quantity)
                    .append("Price", product.getPrice()));
        }

        Document newTicket = new Document("ticketID", nextTicketID())
                .append("date", ticket.getDate())
                .append("products", productList)
                .append("totalPrice", ticket.getTotal());


        ticketCollection.insertOne(newTicket);
    }

    @Override
    public List<Ticket> getAllTickets() {
        List<Ticket> tickets = new ArrayList<>();
        FindIterable<Document> cursor = ticketCollection.find();
        double totalSales = 0;

        for (Document document : cursor) {
            Ticket ticket = documentToTicket(document);
            tickets.add(ticket);
        }
        return tickets;
    }

    @Override
    public void getAllSales(List<Ticket> tickets) {

        double totalSales = 0;
        for (Ticket ticket : tickets) {
            totalSales += ticket.getTotal();
        }

        System.out.println("The total sales of the FlowerShop "
                + FlowerStore.getNameStore() + " is the: " + totalSales + "€.");
    }
}
