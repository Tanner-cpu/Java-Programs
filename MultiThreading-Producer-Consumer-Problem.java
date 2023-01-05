
package program1;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Program1 {
 
//Static class that represents the shared queue between processes    
static class SharedQueue {
    
    private static Queue<Integer> queue; 
    private static Random random; 
    private static int MAX;
    private int consumeCount; 
    private int produceCount;
    private static boolean queueWatch;
    
    //Constructor for private variables 
    public SharedQueue(){
        queue = new LinkedList<>(); 
        random = new Random(); 
        MAX = 500; 
        consumeCount = 0;
        produceCount = 0;
        queueWatch = false;
    
    }
    //Initially set to false, once the producer is finished it is set to true
    synchronized public void queueWatcher(){
        queueWatch = true; 
    }
    
    //Method to keep track of producer count 
    synchronized public int getProduceCount(){
        return produceCount;
    }
    
    //Method to update the producer count 
    synchronized public int updateProduce(){
        produceCount = produceCount + 1; 
        return produceCount; 
    }
    
    //Method to keep track of consumer count 
    synchronized public int getCount(){
        return consumeCount;
    }
    
    //Method to update the consumer count 
    synchronized public int updateCount(){
        consumeCount = consumeCount + 1; 
        return consumeCount; 
    }
} 

//A runnable/thread type that produces a random amount of items for the queue at random intervals
static class Producer implements Runnable{
    
    SharedQueue globalQueue; //Instance of global queue
    lockedImplementation lockedQueue; //Instance of the locking mechanism used for queue
    private int randomTime; 
    
    //Constructor for private variables 
    public Producer(SharedQueue globalQueue, lockedImplementation lockedQueue, int randTime){
        this.globalQueue = globalQueue;
        this.lockedQueue = lockedQueue; 
        randomTime = randTime; 
    }
    
    public void run(){
        int i = 0;
        System.out.println("Starting Producer..."); //Prompts the user that the producer has started
        while(true){ //While true run the process
            if(globalQueue.getProduceCount() != 3){ //The producer will only add values 3 separate times. 
                //if the producer count is not 3, continue...
                if(globalQueue.getProduceCount() != 0){ //if the producer count is not 0, generate a random time...
                    Random rand = new Random(); //(If the producer count is 0, go ahead and skip the sleep portion)
                    randomTime = rand.nextInt(10000);
                  try{ 
                      Thread.sleep(randomTime); //... and sleep for that amount of time 
                  }catch(InterruptedException e){ 
                      throw new RuntimeException();
                }
                }
                    lockedQueue.enqueue(); //Add values to the queue
                    globalQueue.updateProduce();  //Update the produce count by adding 1
                  
            }else{
                System.out.println("Producer has completed its tasks...");//If the producer count is already 3,
                globalQueue.queueWatcher(); //prompt the user and trigger the queueWatcher.
                System.out.println("Queuewatcher has started\n");
                return; //Exit from thread
            }
                 i++;
        }
    }
    
}

//A runnable/thread type that consumes items from a shared queue
static class Consumer implements Runnable{
    
    SharedQueue globalQueue; 
    lockedImplementation lockedQueue;
    private final String myName;
    private final String prefix;
    private final int myTime;
    private int processCount; 
    
    //Constructor for private varaibles 
    public Consumer(SharedQueue globalQueue, lockedImplementation lockedQueue, String name, int timems, String pre, int count){

        this.globalQueue = globalQueue; //Instance of shared queue
        this.lockedQueue = lockedQueue; //Instance of locking mechanism
        myTime = timems; //Sleep time
        myName = name; //Process name 
        prefix = pre; //Prefix 
        processCount = count; //Total processes completed 
    }

    public void run(){
        int i = 0;
        int tempvalue;
        
        System.out.println(prefix + myName + " is Starting..."); //Prompts the user that a specific process is starting
        while(true){
        long startTime = 0; //Initializes time for recording time of individual processes
        long endTime = 0;
            try{
              startTime = System.currentTimeMillis();  //Start stop watch 
              Thread.sleep(myTime); //Sleep or "do work"
              lockedQueue.dequeue(); //Dequeue an item from instance 
              endTime = System.currentTimeMillis(); //Stop stop watch 
            }catch(InterruptedException e){
                throw new RuntimeException();
            }
            tempvalue = globalQueue.updateCount(); //Update count and assign it to a tempvalue
            processCount = processCount + 1; //Update process count 
            long pid = ProcessHandle.current().pid(); //Record current process ID
            //Prompt the user of the activity
            System.out.println(prefix + myName + " finished Process: " + tempvalue + "(" + (endTime-startTime) + " ms) at " + pid); 
            
            //If there is nothing in the queue and produce count is already at three, then end processes
            if(SharedQueue.queue.size() == 0 && globalQueue.getProduceCount() == 3){
                  System.out.println(prefix + myName + " exiting - completed " + processCount + " processes...");
                  return; 
            }else if(SharedQueue.queue.size() == 0){ //Else if the queue is empty but the producer is still working,
                try{
                    System.out.println(prefix + myName + " is idle"); //Make the current process sleep for a given time. 
                    Thread.sleep(myTime);
                }catch(InterruptedException e){
                }
                } 
             i++; 
            }     
        }
    }

//This class is where the majority of the functionality of the shared queue is. It is within it's own class
//becasue I chose to put locks on each functional method for synchronization reasons.
static class lockedImplementation{
    private final Lock lock = new ReentrantLock(); //Instantiate a new lock
    private final Condition bufferFull = lock.newCondition(); //Create two conditions for checking if the 
    private final Condition bufferEmpty = lock.newCondition(); //queue is empty or full
    
    //This method is used to add to the queue 
    public void enqueue(){
        try{
            lock.lock(); //Lock the lock 
            while(SharedQueue.queue.size() == SharedQueue.MAX){ //If the queue is already at its max, wait
                System.out.println("Size of the buffer is maxed out at: " + SharedQueue.queue.size()); 
                bufferFull.await();
            }
                //Numbers used to generate a random number of items to the queue
                int low = 5;
                int high = 25; 
                int additionalProcesses = SharedQueue.random.nextInt(high-low)+low; 
                for(int i = 0; i < additionalProcesses; i++) //Add the random amount of items to the queue
                {
                      SharedQueue.queue.add(1); 
                } 
                System.out.println("\nProducer added " + additionalProcesses + " values in queue."); //Notify the user
                
                //Notify the user of what is left in the queue, and then signalAll that items were added
                System.out.println("Producer thinks there are " + SharedQueue.queue.size() + " Nodes in queue\n");
                bufferEmpty.signalAll();  
        }catch(InterruptedException e){
            throw new RuntimeException();
        }finally{
            lock.unlock(); //Afterwards, unclock the lock for otheer processes
        }    
    }
    
    //This meethod is used to take from the queue 
    public void dequeue(){
        Integer value = 0; //   
        try{
            lock.lock(); //Lock the lock 
            while(SharedQueue.queue.size() == 0){ //If there is nothing in the queue..
                if(SharedQueue.queueWatch == false){  //and if the producer is not finished.. 
                  bufferEmpty.await();//Wait until the producer adds more to the queue
                }else{ //Else if there is nothing in the queue and the producer is finished, then exit process
                    return;
                }    
            }
            value = SharedQueue.queue.poll(); //Take the first value from the queue
            if(value != null){ //If there is a value there, then signalAll that the process took the value
                bufferFull.signalAll();
            }
          }catch(InterruptedException e){
              throw new RuntimeException();
          }finally{
            lock.unlock(); //Unlock the lock
            }
        }       
    }
    
    //Main Program
    public static void main(String[] args) {
        // TODO code application logic here 
        
        SharedQueue globalQueue = new SharedQueue(); //Spawn instance of shared queue
        lockedImplementation lockedQueue = new lockedImplementation(); //Spawn instance of locking mechanism
        
        Producer producer = new Producer(globalQueue, lockedQueue, 200); //Spawn instance of producer
    
        //Spawn two threads for two separate processes
        Consumer consThread1 = new Consumer(globalQueue, lockedQueue, "CPU 1", 1000, "", 0);
        Consumer consThread2 = new Consumer(globalQueue, lockedQueue, "CPU 2", 1000, "              ", 0); 
        
        Thread thread0 = new Thread(producer); 
        Thread thread1 = new Thread(consThread1); 
        Thread thread2 = new Thread(consThread2); 
        
        //Start the producer and 2 consumers 
        thread0.start();
        thread1.start();
        thread2.start(); 
        
       try{
           //Try to join threads
            thread0.join(); 
            thread1.join();
            thread2.join();
       } catch(InterruptedException e){  
       }

       System.out.println("\nMain program exiting");
       System.exit(0); 
    }
    
}
