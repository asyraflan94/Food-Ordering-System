//NAME fund
//INPUT FUND 
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

public class FundAgent extends Agent {

    static final Base64 base64 = new Base64();
    int fund;

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

    //actual coding
    protected void setup() {

        addBehaviour(new Behaviour(this) {
            private int step = 0;

            //must be set to run once only
            public void action() {
                switch (step) {
                    case 0:

                        //receive the msg back from PersonalAgent
                        ACLMessage msg = receive();
                        if (msg != null) {

                            //create numbers object
                            Order order = new Order();
                            //get msg content
                            String msgContent = msg.getContent();
                            try {
                                //cast string to object and set it order object
                                order = (Order) deserializeObjectFromString(msgContent);
                            } catch (Exception ex) {
                            }
                            System.out.println("\n[FundAgent]Fund :" + fund);
                            System.out.println("\n[FundAgent]Total :" + order.getTotal());
                            int total = order.getTotal();
                            if (fund >= total) {

                                ACLMessage msgBA = new ACLMessage(ACLMessage.REQUEST);
                                msgBA.setContent(msgContent);
                                msgBA.addReceiver(new AID("bank", AID.ISLOCALNAME));
                                send(msgBA);
                                System.out.println("Requesting bank ..");
                                step = 1;
                            } else {
                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setContent("Insufficient fund");
                                System.out.println("[FundAgent]Insufficient fund ..");
                                send(reply);
                                step = 2;
                            }

                        }
                        break;
                    case 1:
                        //receive the msg back from BankAgent
                        ACLMessage msgBA = receive();
                        if (msgBA != null) {
                            String msgContent = msgBA.getContent();
                            Order order1 = new Order();
                            //send to personalAgent
                            ACLMessage msgPA = new ACLMessage(ACLMessage.INFORM);
                            if (msgBA.getPerformative() == ACLMessage.INFORM) {
                                try {
                                    //cast string to object and set it order object
                                    order1 = (Order) deserializeObjectFromString(msgContent);
                                } catch (Exception ex) {
                                }
                                System.out.println("\n[FundAgent]Bank Sucessful");
                                fund = fund - order1.getTotal();
                                System.out.println("[FundAgent]Balance Fund :" + fund);
                                msgPA.setContent(msgContent);
                                msgPA.addReceiver(new AID("customer", AID.ISLOCALNAME));
                                step = 2;
                            } //if unsuccessful
                            else {
                                System.out.println("\n[FundAgent]Bank Unsucessful");
                                msgPA.setContent(msgContent);
                                msgPA.addReceiver(new AID("customer", AID.ISLOCALNAME));
                                step = 2;
                            }
                            send(msgPA);
                        }
                        break;

                }

            }

            //make sure your action have code so that done will return true
            public boolean done() {
                return step == 2;
            }

            //delete agent from jade platform
//                    public int onEnd() {
//                        myAgent.doDelete();
//                        return super.onEnd();
//                    }            
        });
        Object[] args = getArguments();
        fund = Integer.parseInt((String) args[0]);
        System.out.println("\n[FundAgent]Fund : RM" + fund);
    }
}
