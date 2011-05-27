package com.frostwire.bittorrent;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.config.impl.ConfigurationParameterNotFoundException;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.tracker.host.TRHostException;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.TrackersUtil;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledTextField;
import com.limegroup.gnutella.gui.LimeTextField;
import com.limegroup.gnutella.settings.SharingSettings;

public class CreateTorrentDialog extends JDialog {

	/**
	 * TRACKER TYPES
	 */
	static final int TT_LOCAL = 1; // I Don't Think So
	static final int TT_EXTERNAL = 2;
	static final int TT_DECENTRAL = 3;
	
	public enum TriggerInThread {
		SWT_THREAD, ANY_THREAD, NEW_THREAD
	}

	static final String TT_EXTERNAL_DEFAULT = "http://";

	/** dht:// */
	static final String TT_DECENTRAL_DEFAULT = TorrentUtils
			.getDecentralisedEmptyURL().toString();

	private static String default_open_dir = COConfigurationManager
			.getStringParameter("CreateTorrent.default.open", "");

	private static String default_save_dir = SharingSettings.TORRENTS_DIR_SETTING
			.getValueAsString();// COConfigurationManager.getStringParameter(
								// "CreateTorrent.default.save", "" );
	private static String comment = I18n
			.tr("Torrent File Created with FrostWire");
	private static int tracker_type = COConfigurationManager.getIntParameter(
			"CreateTorrent.default.trackertype", TT_EXTERNAL);

	// false : singleMode, true: directory
	boolean create_from_dir;
	String singlePath = "";
	String directoryPath = "";
	String savePath = "";

	String trackerURL = TT_EXTERNAL_DEFAULT;

	boolean computed_piece_size = true;
	long manual_piece_size;

	boolean useMultiTracker = false;
	boolean useWebSeed = false;
	private boolean addOtherHashes = false;

	String multiTrackerConfig = "";
	List trackers = new ArrayList();

	String webSeedConfig = "";
	Map webseeds = new HashMap();

	boolean autoOpen = false;
	boolean autoHost = false;
	boolean permitDHT = true;
	boolean privateTorrent = false;

	TOTorrentCreator creator = null;

	private LabeledTextField trackersTextField;
	LimeTextField tt;

	public CreateTorrentDialog() {
		try {
			addOtherHashes = ConfigurationDefaults.getInstance()
					.getBooleanParameter("CreateTorrent.default.addhashes");
		} catch (ConfigurationParameterNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		trackers.add(new ArrayList());

		trackersTextField = new LabeledTextField(I18n.tr("Trackers"), 80);

		// IDEA: If user has URL(s) in clipboard, autofill trackers textfield

		// trackersTextField.setText();
		// trackerURL = Utils.getLinkFromClipboard(display,false);

	}
	
	protected int getTrackerType() {
		return (tracker_type);
	}

	protected void setPieceSizeComputed() {
		computed_piece_size = true;
	}

	public boolean getPieceSizeComputed() {
		return (computed_piece_size);
	}

	protected void setPieceSizeManual(long _value) {
		computed_piece_size = false;
		manual_piece_size = _value;
	}

	protected long getPieceSizeManual() {
		return (manual_piece_size);
	}

	protected void setTrackerType(int type) {
		tracker_type = type;

		COConfigurationManager.setParameter(
				"CreateTorrent.default.trackertype", tracker_type);
	}

	protected String getDefaultSaveDir() {
		return (default_save_dir);
	}

	protected void setDefaultSaveDir(String d) {
		default_save_dir = d;

		COConfigurationManager.setParameter("CreateTorrent.default.save",
				default_save_dir);
	}

	protected void setAddOtherHashes(boolean o) {
		addOtherHashes = o;

		COConfigurationManager.setParameter("CreateTorrent.default.addhashes",
				addOtherHashes);

	}

	protected boolean getAddOtherHashes() {
		return (addOtherHashes);
	}

	public void makeTorrent() {

		int tracker_type = getTrackerType();

		if (tracker_type == TT_EXTERNAL) {
			TrackersUtil.getInstance().addTracker(trackerURL);
		}

		File f;

		if (create_from_dir) {
			f = new File(directoryPath);
		} else {
			f = new File(singlePath);
		}

		try {
			URL url = new URL(trackerURL);

			final TOTorrent torrent;

			if (getPieceSizeComputed()) {

				creator = TOTorrentFactory
						.createFromFileOrDirWithComputedPieceLength(f, url,
								getAddOtherHashes());

				//GUBATRON: NOT SURE IF WE NEED THIS.
				//That addListener Method comes from Wizard and the Listener class has a closed()
				//method that is called when this window is called. I guess they want
				//the torrent creator to notify this window so that it's closed.

				torrent = creator.create();

			} else {
				//GUBATRON: I THINK THIS else WILL NEVER HAPPEN
				//SINCE UI OPTIONS WILL BE A LOT SIMPLER
				TOTorrentCreator c = TOTorrentFactory
						.createFromFileOrDirWithFixedPieceLength(f, url,
								getAddOtherHashes(),
								getPieceSizeManual());

				//c.addListener(this);

				torrent = c.create();
			}

			if (tracker_type == TT_DECENTRAL) {

				TorrentUtils.setDecentralised(torrent);
			}

			torrent.setComment(comment);

			TorrentUtils.setDHTBackupEnabled(torrent, permitDHT);

			TorrentUtils.setPrivate(torrent, privateTorrent);

			LocaleTorrentUtil.setDefaultTorrentEncoding(torrent);

			// mark this newly created torrent as complete to avoid rechecking
			// on open

			final File save_dir;

			if (create_from_dir) {
				save_dir = f;
			} else {
				save_dir = f.getParentFile();
			}

			if (useMultiTracker) {
				//TODO: (MIGHT DO) Notify the UI Thread of current progress
				//this.reportCurrentTask(MessageText.getString("wizard.addingmt"));
				
				TorrentUtils.listToAnnounceGroups(trackers, torrent);
			}

			//NO WEB SEEDS FOR THIS RELEASE.
//			if (useWebSeed && webseeds.size() > 0) {
//				this.reportCurrentTask(MessageText
//						.getString("wizard.webseed.adding"));
//
//				Map ws = _wizard.webseeds;
//
//				List getright = (List) ws.get("getright");
//
//				if (getright.size() > 0) {
//
//					for (int i = 0; i < getright.size(); i++) {
//						reportCurrentTask("    GetRight: " + getright.get(i));
//					}
//					torrent.setAdditionalListProperty("url-list",
//							new ArrayList(getright));
//				}
//
//				List webseed = (List) ws.get("webseed");
//
//				if (webseed.size() > 0) {
//
//					for (int i = 0; i < webseed.size(); i++) {
//						reportCurrentTask("    WebSeed: " + webseed.get(i));
//					}
//					torrent.setAdditionalListProperty("httpseeds",
//							new ArrayList(webseed));
//				}
//
//			}

//			this.reportCurrentTask(MessageText.getString("wizard.savingfile"));

			final File torrent_file = new File(savePath);

			torrent.serialiseToBEncodedFile(torrent_file);
			//this.reportCurrentTask(MessageText.getString("wizard.filesaved"));
			switchToClose();

			//if the user wants to start seeding right away
			if (autoOpen) {
				waitForCore(TriggerInThread.NEW_THREAD,
						new AzureusCoreRunningListener() {
							public void azureusCoreRunning(AzureusCore core) {
								boolean default_start_stopped = COConfigurationManager
										.getBooleanParameter("Default Start Torrents Stopped");

								byte[] hash = null;
								try {
									hash = torrent.getHash();
								} catch (TOTorrentException e1) {
								}

								DownloadManager dm = core
										.getGlobalManager()
										.addDownloadManager(
												torrent_file.toString(),
												hash,
												save_dir.toString(),
												default_start_stopped ? DownloadManager.STATE_STOPPED
														: DownloadManager.STATE_QUEUED,
												true, // persistent
												true, // for seeding
												null); // no adapter required

								if (!default_start_stopped && dm != null) {
									// We want this to move to seeding ASAP, so
									// move it to the top
									// of the download list, where it will do
									// the quick check and
									// move to the seeding list
									// (the for seeding flag should really be
									// smarter and verify
									// it's a seeding torrent and set
									// appropriately)
									dm.getGlobalManager().moveTop(
											new DownloadManager[] { dm });
								}

								if (autoHost && getTrackerType() != TT_EXTERNAL) {

									try {
										core.getTrackerHost().hostTorrent(
												torrent, true, false);

									} catch (TRHostException e) {
										Logger.log(new LogAlert(
												LogAlert.REPEATABLE,
												"Host operation fails", e));
									}
								}

							}
						});
			}
		} catch (Exception e) {
			if (e instanceof TOTorrentException) {

				TOTorrentException te = (TOTorrentException) e;

				if (te.getReason() == TOTorrentException.RT_CANCELLED) {

					// expected failure, don't log exception
				} else {

//					reportCurrentTask(MessageText
//							.getString("wizard.operationfailed"));
//					reportCurrentTask(TorrentUtils.exceptionToText(te));
				}
			} else {
				Debug.printStackTrace(e);
//				reportCurrentTask(MessageText
//						.getString("wizard.operationfailed"));
//				reportCurrentTask(Debug.getStackTrace(e));
			}

			switchToClose();
		}
	}

	/**
	 * Not sure if we need to implement this, I suppose this changed one of the buttons of the wizard from
	 * next|cancel to close
	 */
	private void switchToClose() {
		// TODO Auto-generated method stub
		System.out.println("CreateTorrentDialog.switchToClose() UNIMPLEMENTED.");
		
	}
	
	public static void waitForCore(final TriggerInThread triggerInThread,
			final AzureusCoreRunningListener l) {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				if (triggerInThread == TriggerInThread.ANY_THREAD) {
					l.azureusCoreRunning(core);
				} else if (triggerInThread == TriggerInThread.NEW_THREAD) {
					new AEThread2("CoreWaiterInvoke", true) {
						public void run() {
							l.azureusCoreRunning(core);
						}
					}.start();
				}

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						// TODO: Need to detect cancel (can't rely on shell status since it may never open)
//						if (shell != null && !shell.isDisposed()) {
//							shell.dispose();
//							shell = null;
//						}

						if (triggerInThread == TriggerInThread.SWT_THREAD) {
							l.azureusCoreRunning(core);
						}
					}
				});
			}
		});

		if (!AzureusCoreFactory.isCoreRunning()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					showWaitWindow();
				}
			});
		}

	}

	//TODO
	protected static void showWaitWindow() {
//		if (shell != null && !shell.isDisposed()) {
//			shell.forceActive();
//			return;
//		}
		
//		shell = UIFunctionsManagerSWT.getUIFunctionsSWT().showCoreWaitDlg();
	}

}