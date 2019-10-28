package foodvendor;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.*;
import java.util.Random;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;

//infrom food available
//receive inform payment done or failure from PA
public class FoodvendorAgent extends Agent {

    static final Base64 base64 = new Base64();
    // The catalogue of food for sale (maps the title of a book to its price)
    private Hashtable catalogue;
    // The GUI by means of which the user can add food in the catalogue
    private FoodAgentGui myGui;

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

    //will be invoke in the FoodAgentGui
    public void updateCatalogue(final String name, final int price) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogue.put(name, new Integer(price));
                System.out.println(name + " inserted into catalogue. Price = " + price);
            }
        });
    }

    //generate random transaction code
    public String transactionCode() {
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        return generatedString;
    }

    //ACTUAL CODING
    protected void setup() {

        // Register the food-agent service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("foodvendor-agent");
        sd.setName("JADE-foodvendor-agent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Create the catalogue
        catalogue = new Hashtable();
        // Create and show the GUI 
        myGui = new FoodAgentGui(this);
        myGui.showGui();
        addBehaviour(new OfferRequestsServer());

    }

    private class OfferRequestsServer extends Behaviour {

        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    ACLMessage msg = receive();
                    Food food = new Food();
                    Order order = new Order();
                    if (msg != null) {
                        // CFP Message received. Process it
                        String msgContent = msg.getContent();
                        try {
                            order = (Order) deserializeObjectFromString(msgContent);
                        } catch (Exception ex) {
                            System.out.println("[Agent] StrToObj conversion error :" + ex.getMessage());
                        }
                        food = order.getFood();
                        String name = food.getName();
                        int qty = order.getQty();
                        ACLMessage reply = msg.createReply();
                        Integer price = (Integer) catalogue.get(name);
                        if (price != null) {
                            //total price
                            int total = price * qty;
                            String tc = transactionCode();
                            //store inside object
                            food.setPrice(price);
                            order.setTotal(total);
                            order.setTransCode(tc);
                            String strObj = "";
                            try {
                                strObj = serializeObjectToString(order);
                            } catch (Exception ex) {
                            }
                            // The requested food is available for sale. Reply with the food
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent(strObj);
                        } else {
                            // The requested book is NOT available for sale.
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("food-not-available");
                        }
                        myAgent.send(reply);
                        step = 1;
                    } else {
                        block();
                    }

                    break;

                case 1:
                    ACLMessage msgTA = receive();
                    if (msgTA != null) {
                        String msgContent = msgTA.getContent();
                        // Reply received
                        if (msgTA.getPerformative() == ACLMessage.INFORM) {
                            System.out.println("\n[FoodvendorAgent]Payment Succesful");
                            System.out.println("[FoodvendorAgent]Please enjoy your food and dont forget to say Bismillah");
                        } //payment fail
                        else {
                            System.out.println("\n[FoodvendorAgent]Payment Failed!");
                            System.out.println(msgContent);
                            step = 2;
                        }
                    } else {
                        block();
                    }

                    break;
            }

        }

        public boolean done() {
            return step == 2;
        }
    }  // End of inner class OfferRequestsServer

}
