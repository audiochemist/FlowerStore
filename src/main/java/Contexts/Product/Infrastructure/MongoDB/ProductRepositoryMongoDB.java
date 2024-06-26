package Contexts.Product.Infrastructure.MongoDB;

import Contexts.Product.Domain.*;
import Contexts.Product.Infrastructure.MongoDB.Stock.PrimaryStock;
import Infrastructure.Connections.MongoDBConnection;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProductRepositoryMongoDB<T> implements ProductsRepository {

    private MongoDBConnection mongoDBConnection;
    private MongoCollection<Document> collection;

    public ProductRepositoryMongoDB(MongoDBConnection mongoDBConnection) {
        this.mongoDBConnection = mongoDBConnection;
        this.collection = mongoDBConnection.mongoDatabase.getCollection("products");

    }

    public void initialize() {
        if (collection != null) {
            addPrimaryStock();
        }
    }

    private int nextProductId() {
        Product lastProduct = getLastProduct();
        if (lastProduct == null) {
            return 1;
        } else {
            return lastProduct.getProductId() + 1;
        }
    }

    @Override
    public Product getProduct(int productId) {

        Document query = new Document("productId", productId);
        Document document = collection.find(query).first();
        return documentToProduct(document);
    }


    @Override
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        FindIterable<Document> cursor = collection.find().sort(new Document("type", -1).append("productId", 1));
        for (Document document : cursor) {
            Product product = documentToProduct(document);
            products.add(product);
        }


        return products;
    }

    @Override
    public Product getLastProduct() {
        Document document = collection.find().sort(new Document("productId", -1)).first();

        if (document == null) {
            return null;
        } else {
            return documentToProduct(document);
        }
    }

    public List<Product> getFlowers() {
        List<Product> allProducts = getAllProducts();
        return allProducts.stream()
                .filter(product -> product.getType() == ProductType.FLOWER)
                .collect(Collectors.toList());
    }

    public List<Product> getTrees() {
        List<Product> allProducts = getAllProducts();
        return allProducts.stream()
                .filter(product -> product.getType() == ProductType.TREE)
                .collect(Collectors.toList());
    }

    public List<Product> getDecorations() {
        List<Product> allProducts = getAllProducts();
        return allProducts.stream()
                .filter(product -> product.getType() == ProductType.DECORATION)
                .collect(Collectors.toList());
    }


    @Override
    public void addPrimaryStock() {
        PrimaryStock primaryStock = new PrimaryStock(mongoDBConnection);
        primaryStock.loadInitialStock();
    }


    @Override
    public void addProduct(Product product) {
        Document productDocument = new Document("name", product.getName())
                .append("productId", nextProductId())
                .append("quantity", product.getQuantity())
                .append("price", product.getPrice())
                .append("type", product.getType().toString())
                .append("attribute", product.getAttributes());
        collection.insertOne(productDocument);
    }

    @Override
    public void updateProduct(Product product) {
        Document query = new Document("name", product.getName()).append("attribute", product.getAttributes());
        Document update = new Document("$set", new Document("quantity", product.getQuantity())
                .append("price", product.getPrice()));

        collection.updateOne(query, update);
        System.out.println("Stock updated from " + product.getName());
    }

    @Override
    public void deleteProduct(Product product) {
        Document query = new Document("name", product.getName())
                .append("attribute", product.getAttributes());
        collection.deleteOne(query);
        System.out.println("Product: " + product.getName() + " deleted from DataBase");
    }


    private Product documentToProduct(Document document) {
        int productId = document.getInteger("productId");
        String name = document.getString("name");
        int quantity = document.getInteger("quantity");
        double price = document.getDouble("price");
        ProductType type = ProductType.valueOf(document.getString("type").toUpperCase());
        Object attribute = document.get("attribute");
        return createProduct(productId, name, quantity, price, type, attribute);
    }

    private Product createProduct(int productId, String name, int quantity, double price, ProductType type, Object attribute) {
        switch (type) {
            case FLOWER:
                return new Flower<>(productId, name, quantity, price, attribute);
            case DECORATION:
                return new Decoration<>(productId, name, quantity, price, attribute);
            case TREE:
                return new Tree<>(productId, name, quantity, price, (Double) attribute);
            default:
                throw new IllegalArgumentException("Invalid attribute type for product type " + type);
        }
    }
}

