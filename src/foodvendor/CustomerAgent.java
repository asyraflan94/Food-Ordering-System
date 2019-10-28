/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

public class CustomerAgent extends Agent {

    static final Base64 base64 = new Base64();
    private AID[] foodSeller;
    private String targetFood;
    private String importantMsgContent;

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
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("foodvendor-customer");
        sd.setName("JADE-foodvendor-customer");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        Object[] args = getArguments();
        targetFood = (String) args[1];
        System.out.println("Trying to buy " + targetFood + " meal...");

        addBehaviour(new TickerBehaviour(this, 30000) { //30 sec delay
            protected void onTick() {
                addBehaviour(new RequestPerformer());
            }
        });
    }

    private class RequestPerformer extends Behaviour {

        private AID bestSeller; // The agent who provides the best offer 
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0; // The counter of replies from seller agents
        private int bestPrice;  // The best offered price
        private String transCode;  // The best offered price

        private int step = 0;
        boolean done = false;

        public void action() {
            switch (step) {
                case 0:
                    //get 2 parameters
                    Object[] args = getArguments();
                    int qty = Integer.parseInt((String) args[0]);
                    String name = (String) args[1];

                    //create Ticket object
                    Food food = new Food();
                    Order order = new Order();

                    //set the 3 numbers to the Numbers object
                    food.setName(name);
                    order.setQty(qty);
                    order.setFood(food);

                    //convert object to string
                    String strObj = "";
                    try {
                        strObj = serializeObjectToString(order);
                    } catch (Exception ex) {
                    }

                    System.out.println("\n[CustomerAgent]ORDER");
                    System.out.println("[CustomerAgent]Food name:" + name);
                    System.out.println("[CustomerAgent]Quantity:" + qty);
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent(strObj);

                    //search available agent
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("foodvendor-agent");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found the following food vendor agents:");
                        foodSeller = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            foodSeller[i] = result[i].getName();
                            System.out.println(foodSeller[i].getName());
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    for (int i = 0; i < foodSeller.length; ++i) {
                        msg.addReceiver(foodSeller[i]);
                    }
                    send(msg);
                                // Prepare the template to get proposals

                    step = 1;
                    break;

                case 1:
                    // Receive all inform/refusals from TicketAgents
                    ACLMessage msgTA = receive();
                    if (msgTA != null) {
                        Order order1 = new Order();
                        if (msgTA.getPerformative() == ACLMessage.INFORM) {
                            String msgContent = msgTA.getContent();
                            //convert/cast the string to Numbers object
                            try {
                                order1 = (Order) deserializeObjectFromString(msgContent);
                            } catch (Exception ex) {
                                System.out.println("[CustomerAgent] StrToObj conversion error :" + ex.getMessage());
                            }

                            if (bestSeller == null || order1.getTotal() < bestPrice) {
                                // This is the best offer at present
                                bestPrice = order1.getTotal();
                                bestSeller = msgTA.getSender();
                                transCode = order1.getTransCode();
                                importantMsgContent = msgContent;
                            }

                        }
                        repliesCnt++;
                        if (repliesCnt >= foodSeller.length) {
                            System.out.println("\nORDER DETAILS");
                            System.out.println("[CustomerAgent]Buying from :" + bestSeller);
                            System.out.println("[CustomerAgent]Transaction Code :" + transCode);
                            System.out.println("[CustomerAgent]Total Price : RM " + bestPrice);
                            ACLMessage msgFA = new ACLMessage(ACLMessage.REQUEST);
                            msgFA.setContent(importantMsgContent);
                            msgFA.addReceiver(new AID("fund", AID.ISLOCALNAME));
                            send(msgFA);
                            // We received all replies
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    // receive from FundAgent
                    ACLMessage msgFA = receive();
                    if (msgFA != null) {
                        String msgContent = msgFA.getContent();
                        // Reply received
                        System.out.println("\n[CustomerAgent]Receiving from FundAgent");
                        if (msgFA.getPerformative() == ACLMessage.INFORM) {
                            System.out.println("[CustomerAgent]Payment Accepted");
                            ACLMessage msgT = new ACLMessage(ACLMessage.INFORM);
                            msgT.setContent(msgContent);
                            msgT.addReceiver(bestSeller);
                            send(msgT);
                            step = 3;
                        } //if no ticket
                        else {
                            ACLMessage msgT = new ACLMessage(ACLMessage.REFUSE);
                            msgT.setContent(msgContent);
                            msgT.addReceiver(bestSeller);
                            send(msgT);
                            step = 3;
                        }

                    } else {
                        block();
                    }

                    break;
            }
        }

        public boolean done() {
            return step == 3;
        }

        public int onEnd() {
            myAgent.doDelete();
            return super.onEnd();
        }
    }  // End 
}
