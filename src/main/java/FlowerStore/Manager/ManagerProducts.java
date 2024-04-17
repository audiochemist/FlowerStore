package FlowerStore.Manager;

import Contexts.Product.Domain.*;
import FlowerStore.FlowerStore;
import Utils.InputControl.InputControl;

import java.util.ArrayList;
import java.util.List;

public class ManagerProducts {

    private static ManagerProducts instance;
    private ProductsRepository productsRepository;

    private ManagerProducts(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    public static ManagerProducts getInstance(ProductsRepository productsRepository) {
        if (instance == null) {
            instance = new ManagerProducts(productsRepository);
        }
        return instance;
    }


    public void updateStock() {

        Product product = getProduct();

        int stockToAdd = InputControl.readInt("You selected " + product.getName() + "\n" +
                "Please enter new stock for the product:");
        if (stockToAdd > 0) {
            product.setQuantity(stockToAdd);
            double price = InputControl.readDouble("Choose a price for " + product.getName());
            product.setPrice(price);
            productsRepository.updateProduct(product);
            System.out.println(product.getName() + " updated.");
        } else {
            System.err.println("Can't add under 0");
        }
    }

    public void deleteProduct() {

        Product product = getProduct();
        productsRepository.deleteProduct(product);
    }

    public void addProduct() {
        final int MAX_OPTION = 3;
        int type = InputControl.readIntInRange("\nType\n" +
                "1 for Tree.\n" +
                "2 for Flower.\n" +
                "3 for Decoration", MAX_OPTION);
        String name = InputControl.readString("Type a name for product.");
        int quantity = InputControl.readInt("Type a quantity stock.");
        double price = InputControl.readDouble("Type a price.");

        String typeProduct = "";
        switch (type) {
            case 1:
                typeProduct = ProductType.TREE.toString();
                double attribute = InputControl.readDouble("Type height for the tree");
                Tree newTree = new Tree<>(name, quantity, price, attribute);
                productsRepository.addProduct(newTree);
                break;
            case 2:
                typeProduct = ProductType.FLOWER.toString();
                String flowerAttribute = InputControl.readString("Type color for the flower");
                Flower newFlower = new Flower<>(name, quantity, price, flowerAttribute);
                productsRepository.addProduct(newFlower);
                break;
            case 3:
                typeProduct = ProductType.DECORATION.toString();
                String decorationAttribute;
                Decoration newDecoration;
                boolean validMaterial;

                do {
                    decorationAttribute = InputControl.readString("Type material for the decoration\n" +
                            "Only \"madera\" or \"plastico\"");
                    validMaterial = decorationAttribute.equalsIgnoreCase("madera")
                            || decorationAttribute.equalsIgnoreCase("plastico");
                    if (!validMaterial) {
                        System.out.println("Invalid material. Please enter either \"madera\" or \"plastico\".");
                    }
                } while (!validMaterial);
                newDecoration = new Decoration<>(name, quantity, price, decorationAttribute);
                productsRepository.addProduct(newDecoration);
                break;
        }
        System.out.println(name + " was added");
    }

    public Product getProduct() {

        Product selectProduct;
        List<Product> products = getType();
        showTypeProducts(products);
        int firstID = products.get(0).getProductId();
        int lastID = 0;

        for (int i = 0; i < products.size(); i++) {
            if (firstID > products.get(i).getProductId()) {
                firstID = products.get(i).getProductId();
            }
            if (lastID < products.get(i).getProductId()) {
                lastID = products.get(i).getProductId();
            }
        }
        boolean correctID = false;
        int typeId;

        do {
            correctID = false;
            typeId = InputControl.readInt("Type the ID of the product to select: ");
            if (typeId < firstID || typeId > lastID) {
                System.err.println("Invalid ID. Please enter a valid ID.");
            } else {
                int i = 0;
                while (i < products.size() && !correctID) {
                    if (typeId == products.get(i).getProductId()) {
                        correctID = true;
                    }
                    i++;
                }
                if (!correctID) {
                    System.err.println("Invalid ID. Please enter a valid ID.");
                }
            }
        } while (!correctID);
        selectProduct = productsRepository.getProduct(typeId);
        return selectProduct;
    }

    public void totalValue() {
        List<Product> products = productsRepository.getAllProducts();
        double price = 0;
        for (Product product : products) {
            price += product.getPrice() * product.getQuantity();
        }

        System.out.println("The flower store " + FlowerStore.getNameStore() +
                " had a total value " + price + "€");
    }

    public void stockValue() {
        List<Product> products = getType();
        double priceStock = 0;
        int stock = 0;

        for (Product product : products) {
            priceStock += product.getPrice() * product.getQuantity();
            stock += product.getQuantity();
        }
        showProducts(products);
        System.out.println("\nThe flower store " + FlowerStore.getNameStore()
        + " had a stock value: \n" +
                "TOTAL TYPE STOCK: " + stock +
                "\nPRICE STOCK TYPE: " + priceStock + "€.\n");
    }

    public List<Product> getType() {

        List<Product> products = new ArrayList<>();

        final int MAX_OPTION = 3;
        int option = InputControl.readIntInRange("What you want?\n" +
                "1. TREE.\n" +
                "2. FLOWER.\n" +
                "3. DECORATION.\n", MAX_OPTION);
        switch (option) {

            case 1:
                products = productsRepository.getTrees();
                break;
            case 2:
                products = productsRepository.getFlowers();
                break;
            case 3:
                products = productsRepository.getDecorations();
                break;
        }
        return products;
    }

    public void showProducts(List<Product> products) {
        int idWidth = 5, nameWidth = 15, quantityWidth = 10, priceWidth = 10,
                typeWidth = 15, attributeWidth = 15;

        System.out.printf("%-" + idWidth + "s %-" + nameWidth + "s %-" + quantityWidth
                        + "s %-" + priceWidth + "s %-" + typeWidth + "s %-" + attributeWidth + "s%n",
                "ID", "Name", "Quantity", "Price", "Type", "Attributes");

        System.out.printf("%-" + (idWidth + nameWidth + quantityWidth + priceWidth + typeWidth
                + attributeWidth + 10) + "s%n", "");

        for (Product product : products) {
            System.out.printf("%-" + idWidth + "d %-" + nameWidth + "s %-" + quantityWidth
                            + "d %-" + priceWidth + ".2f %-" + typeWidth + "s %-" + attributeWidth + "s%n",
                    product.getProductId(),
                    product.getName(),
                    product.getQuantity(),
                    product.getPrice(),
                    product.getType().toString(),
                    product.getAttributes());
        }
    }

    public void showTypeProducts(List<Product> products) {
        showProducts(products);
    }

    public void showAllProducts() {
        List<Product> products = productsRepository.getAllProducts();
        if (products.isEmpty()) {
            System.out.println("No products found");
        } else {
            showProducts(products);

        }
    }
}
