package csc311;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CoscoSimulation {
    static final int MS_PER_MIN = 20;
    static final double WAGE_PER_HOUR = 18.0;
    static final int SERVICE_MIN = 2, SERVICE_MAX = 3;

    static class Result{
        double avgWaitMin;
        double maxWaitMin;
        double utilization;
        double cost;
        double totalMin;
    }

    static Result run(int customers, int cashiers, int queueSize) throws InterruptedException {
        CheckoutQueue queue = new CheckoutQueue(queueSize);
        Cashier.Stats stats = new Cashier.Stats();
        long start = System.nanoTime();

        List<Cashier> cashierObjs = new ArrayList<>();
        List<Thread> cashierThreads = new ArrayList<>();
        for (int i = 0; i < cashiers; i++) {
            Cashier c = new Cashier(i +1, queue, SERVICE_MIN * MS_PER_MIN,
                    SERVICE_MAX * MS_PER_MIN, stats);
            Thread t = new Thread(c, "cashier-" + (i + 1));
            cashierObjs.add(c);
            cashierThreads.add(t);
            t.start();
        }

        List<Thread> customerThreads = new ArrayList<>();
        for (int i = 1; i < customers; i++) {
            Thread t = new Thread(new Customer(i, queue, customers * MS_PER_MIN), "customer-" + i);
            customerThreads.add(t);
            t.start();
        }

        for (Thread t : customerThreads) {
            t.join();
        }

        for (int i = 0; i < cashiers; i++) {
            queue.join(Cashier.CLOSE);
        }

        for (Thread t : cashierThreads) {
            t.join();
        }

        double totalMs = (System.nanoTime() - start) / 1000000.0;
        Result r = new Result();
        r.totalMin = totalMs / MS_PER_MIN;
        r.avgWaitMin = stats.served.get() == 0 ? 0 : (double) stats.totalWait.get() / stats.served.get() / MS_PER_MIN;
        r.maxWaitMin = (double) stats.maxWait.get() / MS_PER_MIN;
        long busy = cashierObjs.stream().mapToLong(Cashier::getBusyMillis).sum();
        r.utilization = 100.0 * busy / (totalMs * cashiers);
        r.cost = cashiers * WAGE_PER_HOUR * (r.totalMin / 60.0);
        return r;
    }


    static void print (String label, int cust, int cash, int q, Result r){
        System.out.printf("%-22s cust=%-3d cashiers=%-2d queue=%-3d avgWait=%5.1f min  maxWait=%5.1f min  util=%5.1f%%  time=%5.1f min  cost=$%6.2f%n",
                label, cust, cash, q, r.avgWaitMin, r.maxWaitMin, r.utilization, r.totalMin, r.cost);
    }
    public static void main( String[] args ) throws InterruptedException {
        if(args.length > 0 && args[0].equals("--batch")) {
            print("1 Understaffed", 20, 1, 20, run(20, 1, 20));
            print("2 Optimal", 60, 3, 60, run(60, 3, 60));
            print("3 Overstaffed", 60, 10, 60, run(60, 10, 60));
            print("4 Small queue", 60, 3, 2, run(60, 3, 2));
            print("5 Large queue", 60, 3, 60, run(60, 3, 60));
            for (int c = 2; c <= 6; c++) print("Sweep 60 cust", 60, c, 60, run(60, c, 60));
            return;
        }
        Scanner in = new Scanner(System.in);
        System.out.print("Number of customers: ");  int customers = in.nextInt();
        System.out.print("Number of cashiers: ");   int cashiers = in.nextInt();
        System.out.print("Queue capacity: ");       int q = in.nextInt();
        print("Your run", customers, cashiers, q, run(customers, cashiers, q));
    }


}
