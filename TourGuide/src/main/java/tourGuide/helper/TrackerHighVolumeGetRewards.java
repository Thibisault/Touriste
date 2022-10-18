package tourGuide.helper;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import lombok.Data;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.tracker.Tracker;
import tourGuide.user.User;

@Data
public class TrackerHighVolumeGetRewards extends Thread {
    private final Logger logger = LoggerFactory.getLogger(Tracker.class);
    private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
    //private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final RewardsService rewardsService;
    private boolean stop = false;
    private List<User> userTreatement = new ArrayList<>();
    GpsUtil gpsUtil = new GpsUtil();


    public void setUserTreatement(List<User> sublistUserTreatement) {
        this.userTreatement.addAll(new ArrayList<User>(sublistUserTreatement));
    }

    public TrackerHighVolumeGetRewards(RewardsService rewardsService) {
        this.rewardsService = rewardsService;
        //executorService.submit(this);
    }

    /**
     * Assures to shut down the Tracker thread
     */
    public void stopTracking() {
        logger.debug("Tracker stopping (0)");
        stop = true;
        //executorService.shutdownNow();
    }

    @Override
    public void run() {
        StopWatch stopWatch = new StopWatch();
        logger.info("Nombre de User dans la liste = " +userTreatement.size());
        while(true) {
            if (Thread.currentThread().isInterrupted() || stop) {
                logger.debug("Tracker stopping (1)");
                break;
            }


            logger.debug("Begin Tracker. Tracking " + userTreatement.size() + " users.");
            Attraction attraction = gpsUtil.getAttractions().get(0);
            userTreatement.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));
            stopWatch.start();
            userTreatement.forEach(u -> rewardsService.calculateRewards(u));
            stopWatch.stop();

            logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
            stopWatch.reset();
            break;
        }
    }
}
