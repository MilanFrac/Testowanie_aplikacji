import org.example.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.quartz.impl.JobExecutionContextImpl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;

public class testy_jednostkowe {
    private Main.DataCollectionJob dataCollectionJob;
    private JobExecutionContext jobExecutionContextMock;
    private Main.Product product;

    @BeforeEach
    public void setup() {
        dataCollectionJob = new Main.DataCollectionJob();
        jobExecutionContextMock = mock(JobExecutionContextImpl.class);
        product = new Main.Product("https://mediamarkt.pl/telefony-i-smartfony/smartfon-apple-iphone-14-pro-128gb-gwiezdna-czern-mpxv3px-a-1");
    }

    @Test
    public void testDataCollectionJobExecute_SuccessfulExecution() {
        // Arrange
        Main.PriceChange priceChange = new Main.PriceChange("Test Product", 100, new Date(), List.of());
        Main.DataCollectionJob dataCollectionJobSpy = spy(dataCollectionJob);
        doReturn(priceChange).when(dataCollectionJobSpy).writePriceChangeToFile(any());

        // Act
        dataCollectionJobSpy.execute(jobExecutionContextMock);

        // Assert
        verify(dataCollectionJobSpy, times(1)).writePriceChangeToFile(priceChange);
    }

    @Test
    public void testDataCollectionJobExecute_NoProductFound() {
        // Arrange
        Main.DataCollectionJob dataCollectionJobSpy = spy(dataCollectionJob);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        // Act
        dataCollectionJobSpy.execute(jobExecutionContextMock);

        // Assert
        String consoleOutput = outputStream.toString();
        Assertions.assertTrue(consoleOutput.contains("Nie znaleziono żadnego produktu na stronach."));
    }

    @Test
    public void testParsePrice_ValidPriceText() {
        // Arrange
        String priceText = "100,00 zł";

        // Act
        int price = dataCollectionJob.parsePrice(priceText);

        // Assert
        Assertions.assertEquals(100, price);
    }

    @Test
    public void testParsePrice_InvalidPriceText() {
        // Arrange
        String priceText = "Invalid Price";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outputStream));

        // Act
        int price = dataCollectionJob.parsePrice(priceText);

        // Assert
        Assertions.assertEquals(0, price);
        String errorOutput = outputStream.toString();
        Assertions.assertTrue(errorOutput.contains("Nie można przekształcić ceny na liczbę całkowitą"));
    }

    @Test
    public void testLoadPriceChangeTimes_SuccessfulLoad() {
        // Arrange
        String productName = "Test Product";
        Date changeDate = new Date();

        // Act
        List<Date> changeTimes = dataCollectionJob.loadPriceChangeTimes(productName);

        // Assert
        Assertions.assertEquals(1, changeTimes.size());
    }

    @Test
    public void testWritePriceChangeToFile_SuccessfulWrite() throws ParseException {
        // Arrange
        Main.PriceChange priceChange = new Main.PriceChange("Test Product", 100, new Date(), List.of());

        // Act
        dataCollectionJob.writePriceChangeToFile(priceChange);

        // Add assertions for verifying the file write operation
    }
}
