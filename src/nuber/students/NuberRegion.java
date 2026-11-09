package nuber.students;

import java.util.concurrent.*;

/**
 * A single Nuber region that operates independently of other regions, other than getting 
 * drivers from bookings from the central dispatch.
 * 
 * A region has a maxSimultaneousJobs setting that defines the maximum number of bookings 
 * that can be active with a driver at any time. For passengers booked that exceed that 
 * active count, the booking is accepted, but must wait until a position is available, and 
 * a driver is available.
 * 
 * Bookings do NOT have to be completed in FIFO order.
 * 
 * @author james
 *
 */
public class NuberRegion {

    private final NuberDispatch dispatch;
    private final String regionName;
    private final Semaphore permits;
    private final ExecutorService exec;
    private volatile boolean shuttingDown = false;
    
	/**
	 * Creates a new Nuber region
	 * 
	 * @param dispatch The central dispatch to use for obtaining drivers, and logging events
	 * @param regionName The regions name, unique for the dispatch instance
	 * @param maxSimultaneousJobs The maximum number of simultaneous bookings the region is allowed to process
	 */
	public NuberRegion(NuberDispatch dispatch, String regionName, int maxSimultaneousJobs)
	{
        this.dispatch = dispatch;
        this.regionName = regionName;
        this.permits = new Semaphore(Math.max(1, maxSimultaneousJobs), true); // fair
        this.exec = Executors.newCachedThreadPool();
	}
	
	/**
	 * Creates a booking for given passenger, and adds the booking to the 
	 * collection of jobs to process. Once the region has a position available, and a driver is available, 
	 * the booking should commence automatically. 
	 * 
	 * If the region has been told to shutdown, this function should return null, and log a message to the 
	 * console that the booking was rejected.
	 * 
	 * @param waitingPassenger
	 * @return a Future that will provide the final BookingResult object from the completed booking
	 */
	public Future<BookingResult> bookPassenger(Passenger waitingPassenger)
	{		
        if (shuttingDown) {
            dispatch.logEvent(null, "Booking rejected for region '" + regionName + "' (shutdown)");
            return null;
        }

        Booking booking = new Booking(dispatch, waitingPassenger);

        return exec.submit(() -> {
            permits.acquire();
            try {
                dispatch.logEvent(booking, "Starting (region slot acquired, waiting for driver)");
                BookingResult result = booking.call();
                dispatch.logEvent(booking, "Completed");
                return result;
            } finally {
                permits.release();
            }
        });
	}
	
	/**
	 * Called by dispatch to tell the region to complete its existing bookings and stop accepting any new bookings
	 */
	public void shutdown()
	{
        shuttingDown = true;
        exec.shutdown();
        try {
            exec.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
	}
		
}
