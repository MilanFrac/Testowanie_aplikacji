package org.example;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Main {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_FILE_PATH = "price_change.csv";

    public static void main(String[] args) {
    }

    public static class DataCollectionJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            List<Product> products = new ArrayList<>();
            products.add(new Product("https://mediamarkt.pl/telefony-i-smartfony/smartfon-apple-iphone-14-pro-128gb-gwiezdna-czern-mpxv3px-a-1"));

            try {
                List<PriceChange> priceChanges = new ArrayList<>();
                for (Product product : products) {
                    Document doc = Jsoup.connect(product.url()).get();
                    Elements productElements = doc.select("span.whole");

                    if (!productElements.isEmpty()) {
                        Element productElement = productElements.first();
                        assert productElement != null;
                        String priceText = productElement.text();
                        int price = parsePrice(priceText);

                        List<Date> changeTimes = loadPriceChangeTimes(product.getName());
                        PriceChange priceChange = new PriceChange(product.getName(), price, new Date(), changeTimes);
                        priceChanges.add(priceChange);

                        writePriceChangeToFile(priceChange);
                        generatePriceChart(priceChange);
                    }
                }

                if (!priceChanges.isEmpty()) {
                    System.out.println("Pomyślnie zapisano zmiany cen produktów w plikach CSV i wygenerowano wykresy.");
                } else {
                    System.out.println("Nie znaleziono żadnego produktu na stronach.");
                }

            } catch (IOException e) {
                System.err.println("Wystąpił błąd podczas pobierania danych ze stron: " + e.getMessage());
            }
        }

        public int parsePrice(String priceText) {
            String cleanedPriceText = priceText.replaceAll("[^\\d,]", "");
            cleanedPriceText = cleanedPriceText.replace(",", ".");
            cleanedPriceText = cleanedPriceText.replace(" ", "");

            try {
                return Integer.parseInt(cleanedPriceText);
            } catch (NumberFormatException e) {
                System.err.println("Nie można przekształcić ceny na liczbę całkowitą: " + priceText);
                return 0;
            }
        }

        public List<Date> loadPriceChangeTimes(String productName) {
            List<Date> changeTimes = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 3 && parts[0].equals(productName)) {
                        try {
                            Date changeTime = DATE_FORMAT.parse(parts[2]);
                            changeTimes.add(changeTime);
                        } catch (Exception e) {
                            System.err.println("Błąd podczas parsowania daty: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Wystąpił błąd podczas odczytu pliku: " + e.getMessage());
            }
            return changeTimes;
        }

        private void generatePriceChart(PriceChange priceChange) {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Date changeTime : priceChange.changeTimes()) {
                dataset.addValue(priceChange.price(), priceChange.product(), DATE_FORMAT.format(changeTime));
            }

            JFreeChart chart = ChartFactory.createLineChart(
                    "Zmiany cen produktu: " + priceChange.product(),
                    "Data zmiany",
                    "Cena",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );

            String chartImagePath = "price_chart_" + priceChange.product().replaceAll("\\s", "_") + ".png";
            try {
                ChartUtilities.saveChartAsPNG(new File(chartImagePath), chart, 1200, 600);
                System.out.println("Pomyślnie wygenerowano wykres zmiany cen dla produktu: " + priceChange.product());
            } catch (IOException e) {
                System.err.println("Wystąpił błąd podczas generowania wykresu: " + e.getMessage());
            }
        }

        public void writePriceChangeToFile(PriceChange priceChange) {

        }

    }

    public record Product(String url) {

        public String getName() {
                return "smartfon apple iphone 14 pro 128gb gwiezdna czerń";
            }
        }

    public record PriceChange(String product, int price, Date changeDate, List<Date> changeTimes) {
    }
}