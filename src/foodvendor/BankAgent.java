//NAME bank
//INPUT UP OR DOWN FOR BANK STATUS
package foodvendor;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;

//process payment
public class BankAgent extends Agent {

    private String status;
    static final Base64 base64 = new Base64();

    public String serializeObjectToString(Object object) throws IOException {
        String s = null;

        try {
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(arrayOutputStream);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);

            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            gzipOutputStream.close();

            objectOutputStream.flush();
            objectOutputStream.close();

            s = new String(base64.encode(arrayOutputStream.toByteArray()));
            arrayOutputStream.flush();
            arrayOutputStream.close();
        } catch (Exception ex) {
        }

        return s;
    }

    public Object deserializeObjectFromString(String objectString) throws IOException, ClassNotFoundException {
        Object obj = null;
        try {
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(base64.decode(objectString));
            GZIPInputStream gzipInputStream = new GZIPInputStream(arrayInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
            obj = objectInputStream.readObject();

            objectInputStream.close();
            gzipInputStream.close();
            arrayInputStream.close();
        } catch (Exception ex) {
        }
        return obj;
    }

    protected void setup() {

        addBehaviour(new CyclicBehaviour(this) {

            public void action() {

                //receive the msg back from myag
                Order order = new Order();
                ACLMessage msg = receive();
                if (msg != null) {
                    String msgContent = msg.getContent();
                    try {
                        //cast string to object and set it order object
                        order = (Order) deserializeObjectFromString(msgContent);
                    } catch (Exception ex) {
                        System.out.println("Unable to convert");
                    }
                    ACLMessage reply = msg.createReply();

                    System.out.println("\n[BankAgent]Successful");
                    System.out.println("[BankAgent]Transaction Code :" + order.getTransCode());
                    System.out.println("[BankAgent]Amount :" + order.getTotal());

                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(msgContent);
//                    if (status.equals("up")) {
//                        System.out.println("\n[BankAgent]Successful");
//                        System.out.println("[BankAgent]Transaction Code :" + order.getTransCode());
//                        System.out.println("[BankAgent]Amount :" + order.getTotal());
//
//                        reply.setPerformative(ACLMessage.INFORM);
//                        reply.setContent(msgContent);
//
//                    } else {
//                        reply.setPerformative(ACLMessage.REFUSE);
//                        reply.setContent("\n[BankAgent]ServerDown");
//                    }
                    send(reply);
                }
                block();
            }

        });
//        System.out.println("\n[BankAgent] Please set 'up' or 'down' for bank status ");
//        Object[] args = getArguments();
//        status = (String) args[0];
        System.out.println("[BankAgent] Status : online");
//        System.out.println("[BankAgent] Status :" + status);
        //get bank status

    }
}
