import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * Created by steve on 5/12/16.
 */
public class SerialTest {

    public static void main(String[] args) {

        Runnable r = () -> {

            try

            {
                SerialPort serialPort = new SerialPort("/dev/tty.usbserial-AH01DOWK");
                boolean run = true;
                System.out.println("Port opened: " + serialPort.openPort());
                System.out.println("Params setted: " + serialPort.setParams(
                        SerialPort.BAUDRATE_115200,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE));
                //serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
                StringBuilder sb = new StringBuilder();
                serialPort.addEventListener(new PortReader(serialPort, sb));

                while (run) {
                    //System.out.println("\"Hello World!!!\" successfully writen to port: " + serialPort.writeString("Hello World!!!\r\n"));

                    if (sb.length() > 0) {
                        System.out.println(sb.toString());
                    }

                    //Thread.sleep(5000);
                }


                System.out.println("Port closed: " + serialPort.closePort());
            } catch (Exception ex) {
                System.out.println(ex);
            }

        };

        new Thread(r).start();

    }
}

class PortReader implements SerialPortEventListener {

    private SerialPort sp;
    private StringBuilder sb;

    public PortReader(SerialPort sp, StringBuilder sb) {
        this.sp = sp;
        this.sb = sb;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                sb.append(sp.readString(event.getEventValue()));
            } catch (SerialPortException ex) {
                System.out.println("Error in receiving string from COM-port: " + ex);
            }
        }
    }

}