import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HttpServerTest {

    String[] binarys;

    int[] values;

    @BeforeEach
    public void setup () {

        binarys =new String[] {
                "10101000",
                "11001100",
                "10100101",
                "100101"
        };

        values = new int[] {
                168,
                204,
                165,
                37,
        };

    }

    @Test
    public void testConversionBinToInt() {
        int[] actual = new int[values.length];
        for (int i = 0; i < binarys.length; i++) {
            actual[i] = HttpServer.binaryToInt(binarys[i]);
        }

        assertArrayEquals(values, actual, "Convertion fausse");

    }

    @Test
    public void testConversionIntToBin() {
        String[] actual = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            actual[i] = HttpServer.valueToBinary(values[i]);
        }

        assertArrayEquals(binarys, actual,"Conversion n'est pas bonne");
    }

}