package tourGuide;

import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.tracker.Tracker;
import tourGuide.helper.TrackerHighVolumeGetRewards;
import tourGuide.helper.TrackerHighVolumeTrackLocation;
import tourGuide.user.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPerformance {
	private final Logger logger = LoggerFactory.getLogger(Tracker.class);
	static int numberTotalOfThreads = 1000;
	static final int internalUserNumber = 100;
	static int userInterval = internalUserNumber / numberTotalOfThreads;


	/*
	 * A note on performance improvements:
	 *     
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *
	 *     		InternalTestHelper.setInternalUserNumber(100000);
	 *     
	 *     
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent. 
	 * 
	 *     These are performance metrics that we are trying to hit:
	 *     
	 *     highVolumeTrackLocation:.00 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */
	
	@Test
	public void highVolumeTrackLocation() throws InterruptedException {
		GpsUtil gpsUtil = new GpsUtil();

		if (userInterval == 0){
			userInterval++;
		}
		if (numberTotalOfThreads > internalUserNumber){
			numberTotalOfThreads = internalUserNumber;
		}

		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		// Users should be incremented up to 100,000, and test finishes within 15 minutes
		InternalTestHelper.setInternalUserNumber(internalUserNumber);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		List<TrackerHighVolumeTrackLocation> tableTrackerHighVolumeTrackLocation = new ArrayList<>(numberTotalOfThreads);
		System.out.println("Début boucle new");
		for (int i = 0; i < numberTotalOfThreads; i ++) {
			TrackerHighVolumeTrackLocation trackerHighVolumeTrackLocation = new TrackerHighVolumeTrackLocation(tourGuideService);
			trackerHighVolumeTrackLocation.setName("Thibault-" + i);
			tableTrackerHighVolumeTrackLocation.add(trackerHighVolumeTrackLocation);
		}
		logger.info("Nombre de thread = "+ tableTrackerHighVolumeTrackLocation.size());

		List<User> allUsers = Collections.synchronizedList(tourGuideService.getAllUsers());
		int b = 1;
		for (User user : allUsers){
			user.setName("Prénom User : " + b);
			b++;
		}

		logger.info("Début boucle start");
	    StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		int threadIdx = 0;
		int tailleListe = 0;
		int firstUser = 0;
		int modulo = internalUserNumber % numberTotalOfThreads;
		System.out.println("Modulo : "+modulo);
		for (TrackerHighVolumeTrackLocation trackerHighVolumeTrackLocation : tableTrackerHighVolumeTrackLocation) {
			if (threadIdx < modulo){
				System.out.println("Nombre de passage avant modulo : "+threadIdx);
				trackerHighVolumeTrackLocation.setUserTreatement(allUsers.subList(firstUser, userInterval + firstUser +1));
				trackerHighVolumeTrackLocation.start();
				firstUser = firstUser + userInterval +1;
				tailleListe = tailleListe + tableTrackerHighVolumeTrackLocation.get(threadIdx++).getUserTreatement().size();

				for (User user : trackerHighVolumeTrackLocation.getUserTreatement()) {
					logger.info(user.getName());
				}
			} else {
				System.out.println("Nombre de passage après modulo : "+threadIdx);
				trackerHighVolumeTrackLocation.setUserTreatement(allUsers.subList(firstUser, userInterval + firstUser));
				trackerHighVolumeTrackLocation.start();
				firstUser = firstUser + userInterval;
				tailleListe = tailleListe + tableTrackerHighVolumeTrackLocation.get(threadIdx++).getUserTreatement().size();
				for (User user : trackerHighVolumeTrackLocation.getUserTreatement()) {
					logger.info(user.getName());
				}
			}
		}

		logger.info("Début boucle join");
		for (int i =0; i < tableTrackerHighVolumeTrackLocation.size(); i++){
			logger.info("Attente fin thread " + i);
			tableTrackerHighVolumeTrackLocation.get(i).join();
		}

		stopWatch.stop();
		logger.info("Fin boucle");
		tourGuideService.tracker.stopTracking();
		logger.info("Nombre de UserTreatement passé à la moulinette " + tailleListe);
		logger.info("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	
	@Test
	public void highVolumeGetRewards() throws InterruptedException {
		GpsUtil gpsUtil = new GpsUtil();

		if (userInterval == 0){
			userInterval++;
		}
		if (numberTotalOfThreads > internalUserNumber){
			numberTotalOfThreads = internalUserNumber;
		}

		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		InternalTestHelper.setInternalUserNumber(internalUserNumber);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		List<TrackerHighVolumeGetRewards> tableTrackerHighVolumeGetRewards = new ArrayList<>(numberTotalOfThreads);
		System.out.println("Début boucle new");
		for (int i = 0; i < numberTotalOfThreads; i ++) {
			TrackerHighVolumeGetRewards trackerHighVolumeGetRewards = new TrackerHighVolumeGetRewards(rewardsService);
			trackerHighVolumeGetRewards.setName("Thibault-" + i);
			tableTrackerHighVolumeGetRewards.add(trackerHighVolumeGetRewards);
		}
		logger.info("Nombre de thread = "+ tableTrackerHighVolumeGetRewards.size());


		List<User> allUsers = Collections.synchronizedList(tourGuideService.getAllUsers());
		int b = 1;
		for (User user : allUsers){
			user.setName("Prénom User : " + b);
			b++;
		}

		int threadIdx = 0;
		int tailleListe = 0;
		int firstUser = 0;
		int modulo = internalUserNumber % numberTotalOfThreads;
		System.out.println("Modulo : "+modulo);
		for (TrackerHighVolumeGetRewards trackerHighVolumeGetRewards : tableTrackerHighVolumeGetRewards) {
			if (threadIdx < modulo){
				System.out.println("Nombre de passage avant modulo : "+threadIdx);
				trackerHighVolumeGetRewards.setUserTreatement(allUsers.subList(firstUser, userInterval + firstUser +1));
				trackerHighVolumeGetRewards.start();
				firstUser = firstUser + userInterval +1;
				tailleListe = tailleListe + tableTrackerHighVolumeGetRewards.get(threadIdx++).getUserTreatement().size();

				for (User user : trackerHighVolumeGetRewards.getUserTreatement()) {
					logger.info(user.getName());
				}
			} else {
				System.out.println("Nombre de passage après modulo : "+threadIdx);
				trackerHighVolumeGetRewards.setUserTreatement(allUsers.subList(firstUser, userInterval + firstUser));
				trackerHighVolumeGetRewards.start();
				firstUser = firstUser + userInterval;
				tailleListe = tailleListe + tableTrackerHighVolumeGetRewards.get(threadIdx++).getUserTreatement().size();
				for (User user : trackerHighVolumeGetRewards.getUserTreatement()) {
					logger.info(user.getName());
				}
			}
		}

		logger.info("Début boucle join");
		for (int i =0; i < tableTrackerHighVolumeGetRewards.size(); i++){
			logger.info("Attente fin thread " + i);
			tableTrackerHighVolumeGetRewards.get(i).join();
		}

		for(User user : allUsers) {
			assertTrue(user.getUserRewards().size() > 0);
		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		logger.info("Nombre de UserTreatement passé à la moulinette " + tailleListe);
		logger.info("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
	
}
