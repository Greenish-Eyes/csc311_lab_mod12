package csc311;


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
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
    }
}
