package nuber.students;

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The core Dispatch class that instantiates and manages everything for Nuber
 * 
 * @author james
 *
 */
public class NuberDispatch {

	/**
	 * The maximum number of idle drivers that can be awaiting a booking 
	 */
    private final int MAX_DRIVERS = 999;
    private final boolean logEvents;
	
    private final LinkedBlockingQueue<Driver> idleDrivers = new LinkedBlockingQueue<>(MAX_DRIVERS);
    private final Map<String, NuberRegion> regions = new java.util.concurrent.ConcurrentHashMap<>();
    private final AtomicInteger bookingsAwaitingDriver = new AtomicInteger(0);
    private volatile boolean shuttingDown = false;
	
	/**
	 * Creates a new dispatch objects and instantiates the required regions and any other objects required.
	 * It should be able to handle a variable number of regions based on the HashMap provided.
	 * 
	 * @param regionInfo Map of region names and the max simultaneous bookings they can handle
	 * @param logEvents Whether logEvent should print out events passed to it
	 */
	public NuberDispatch(HashMap<String, Integer> regionInfo, boolean logEvents)
	{
	    this.logEvents = logEvents;

	    System.out.println("Creating Nuber Dispatch");

	    int count = regionInfo == null ? 0 : regionInfo.size();
	    System.out.println("Creating " + count + " regions");

	    if (regionInfo != null) {
	        regionInfo.forEach((name, cap) -> {
	            System.out.println("Creating Nuber region for " + name);
	            int max = (cap == null ? 1 : Math.max(1, cap));
	            regions.put(name, new NuberRegion(this, name, max));
	        });
	    }
	    System.out.println("Done creating " + count + " regions");
	}
	
	/**
	 * Adds drivers to a queue of idle driver.
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @param The driver to add to the queue.
	 * @return Returns true if driver was added to the queue
	 */
	public boolean addDriver(Driver newDriver)
	{
        return newDriver != null && idleDrivers.offer(newDriver);
	}
	
	/**
	 * Gets a driver from the front of the queue
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @return A driver that has been removed from the queue
	 */
	public Driver getDriver()
	{
        while (true) {
            try {
                Driver d = idleDrivers.take();
                bookingsAwaitingDriver.updateAndGet(x -> Math.max(0, x - 1));
                return d;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
	}

	/**
	 * Prints out the string
	 * 	    booking + ": " + message
	 * to the standard output only if the logEvents variable passed into the constructor was true
	 * 
	 * @param booking The booking that's responsible for the event occurring
	 * @param message The message to show
	 */
	public void logEvent(Booking booking, String message) {
		
		if (!logEvents) return;
		
		System.out.println(booking + ": " + message);
		
	}

	/**
	 * Books a given passenger into a given Nuber region.
	 * 
	 * Once a passenger is booked, the getBookingsAwaitingDriver() should be returning one higher.
	 * 
	 * If the region has been asked to shutdown, the booking should be rejected, and null returned.
	 * 
	 * @param passenger The passenger to book
	 * @param region The region to book them into
	 * @return returns a Future<BookingResult> object
	 */
	public Future<BookingResult> bookPassenger(Passenger passenger, String region) {
        if (shuttingDown) {
            logEvent(null, "Booking rejected (dispatch shutdown)");
            return null;
        }
        NuberRegion r = regions.get(region);
        if (r == null) return null;

        bookingsAwaitingDriver.incrementAndGet();
        Future<BookingResult> f = r.bookPassenger(passenger);
        if (f == null) {
            bookingsAwaitingDriver.updateAndGet(x -> Math.max(0, x - 1));
        }
        return f;
	}

	/**
	 * Gets the number of non-completed bookings that are awaiting a driver from dispatch
	 * 
	 * Once a driver is given to a booking, the value in this counter should be reduced by one
	 * 
	 * @return Number of bookings awaiting driver, across ALL regions
	 */
	public int getBookingsAwaitingDriver()
	{
		return bookingsAwaitingDriver.get();
	}
	
	/**
	 * Tells all regions to finish existing bookings already allocated, and stop accepting new bookings
	 */
	public void shutdown() {
        shuttingDown = true;
        regions.values().forEach(NuberRegion::shutdown);
	}

}
