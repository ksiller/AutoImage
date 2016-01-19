package autoimage.gui.dialogs;

import autoimage.gui.models.JobTableModel;
import autoimage.events.job.JobSwitchEvent;
import autoimage.data.acquisition.Job;
import autoimage.data.acquisition.Job.Status;
import autoimage.services.JobManager;
import autoimage.events.job.JobStatusChangedEvent;
import autoimage.events.job.AllJobsCompletedEvent;
import autoimage.events.job.JobImageStoredEvent;
import autoimage.events.job.JobListChangedEvent;
import autoimage.events.job.JobPlacedInQueueEvent;
import com.google.common.eventbus.Subscribe;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.InvalidParameterException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Karsten Siller
 */
public class JobStatusWindow extends JDialog {
    
    private JobManager acqManager;
    private final JTable jobTable;
    private final JButton stopAllButton;
    private final JButton skipButton;
    private Timer scheduleCountDown;
    
    private class JobStatusCellRenderer extends DefaultTableCellRenderer {
        
        private String formatWaitTime(long timeMS) {
            int seconds = (int)Math.floor(timeMS / 1000) % 60 ;
            int minutes = (int) Math.floor(timeMS / (1000*60)) % 60;
            int hours   = (int) Math.floor(timeMS / (1000*60*60)) % 24;
            int days = (int) Math.floor(timeMS / (1000*60*60*24));                                    
            String timeStr="";
            if (days > 0) {
                timeStr=timeStr+Integer.toString(days)+"d ";
            }
            if (hours > 0 | days > 0) {
                timeStr=timeStr+Integer.toString(hours)+"h ";
            }
            if (minutes > 0 || hours > 0 || days > 0) {
                timeStr=timeStr+Integer.toString(minutes)+"min ";
            }
            timeStr=timeStr+Integer.toString(seconds)+"s";
            return timeStr;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Status status=(Status)value;
            Job job=((JobTableModel)jobTable.getModel()).get(row);
            if (job!=null && Job.JobRunning(status)) {
                //show progressbar for running jobs
                Float progress=job.getProgress(status);
                JProgressBar progressbar=new JProgressBar(0,100);
                progressbar.setLayout(new BorderLayout(2,2));
                if (progress!=null && progress > 0) {
                    progressbar.setIndeterminate(false);
                    progressbar.setValue((int)(progress*100));
                } else {
                    progressbar.setIndeterminate(true);
                }
                //add status description to progressbar
                JLabel statusLabel=new JLabel(status.toString());
                progressbar.add(statusLabel,BorderLayout.WEST);
                return progressbar;
            } else {
                if (status==Job.Status.SCHEDULED) { //&& job==acqManager.getCurrentJob()) {
                    //if scheduled (not running) show status description and time count down
                    long delta=job.getScheduledTimeMS()-System.currentTimeMillis();
                    if (delta >= 0) {
                        return super.getTableCellRendererComponent(table, "Start in: "+formatWaitTime(delta), isSelected, hasFocus, row, column);
                    } else {
                        return super.getTableCellRendererComponent(table, "Delayed: "+formatWaitTime(-delta), isSelected, hasFocus, row, column);
                    }
                } else {
                    //just show status description
                    return super.getTableCellRendererComponent(table, status.toString(), isSelected, hasFocus, row, column);
                }
            }    
        }    
    }
    
    
    public JobStatusWindow (Frame p, JobManager manager) {
        super(p);
        if (manager==null) {
            throw new InvalidParameterException("Job Manager cannot be null");
        }
        acqManager=manager;
        setTitle("Job Manager");
        setMinimumSize(new Dimension(500,250));
        SpringLayout springLayout = new SpringLayout();
        getContentPane().setLayout(springLayout);
        
        //buttons
        int buttonHeight=20;
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        stopAllButton = new JButton("Stop All",new ImageIcon(getClass().getResource("/autoimage/resources/delete.png")));
        stopAllButton.setPreferredSize(new Dimension(100, buttonHeight));
        stopAllButton.setEnabled(acqManager!=null);
        stopAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg) {
                new Thread(new Runnable () {
                    @Override
                    public void run() {
                        acqManager.cancelAllJobs();
                    }
                }).start();
            }
        });
        stopAllButton.setToolTipText("Stop all acquisition jobs");
        buttonPanel.add(stopAllButton);

        skipButton = new JButton("Skip", new ImageIcon(getClass().getResource("/autoimage/resources/delete.png")));
        skipButton.setPreferredSize(new Dimension(100, buttonHeight));
        skipButton.setEnabled(acqManager!=null);
        skipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        acqManager.skipToNextJob();
                    }
                }).start();
            }
        });
        skipButton.setToolTipText("Skip to next job");
        buttonPanel.add(skipButton);
        springLayout.putConstraint(SpringLayout.NORTH, buttonPanel, -10-2*buttonHeight, SpringLayout.SOUTH, getContentPane());     
        springLayout.putConstraint(SpringLayout.SOUTH, buttonPanel, -10, SpringLayout.SOUTH, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, buttonPanel, -10, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, buttonPanel, +10, SpringLayout.WEST, getContentPane());
        
        //job table
        jobTable = new JTable(new JobTableModel(acqManager.getJobs()));
        jobTable.setEnabled(false);
        jobTable.getColumnModel().getColumn(1).setCellRenderer(new JobStatusCellRenderer());
        jobTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane=new JScrollPane(jobTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.NORTH, getContentPane());     
        springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -10, SpringLayout.NORTH, buttonPanel);
        springLayout.putConstraint(SpringLayout.EAST, scrollPane, -10, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, scrollPane, +10, SpringLayout.WEST, getContentPane());
                 
        getContentPane().add(scrollPane);
        getContentPane().add(buttonPanel);
        pack();
        
        selectCurrentJob(acqManager.getCurrentJob().getId());
        
        scheduleCountDown = new Timer(0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               SwingUtilities.invokeLater(new Runnable() {

                   @Override
                   public void run() {
                        ((JobTableModel)jobTable.getModel()).updateTable();
                   }

                });
            }
        });
        scheduleCountDown.setDelay(1000);
        scheduleCountDown.start();
        
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (scheduleCountDown!=null) {
                    scheduleCountDown.stop();
                }
            }
        });

    }    
    
    @Subscribe
    public void jobListChanged(JobListChangedEvent e) {
        if (e.getManager()==acqManager) {
            ((JobTableModel)jobTable.getModel()).setJobs(e.getJobs());
            if (!e.getJobs().isEmpty() && !scheduleCountDown.isRunning()) {  
                scheduleCountDown.restart();
            }
        }
    }
    
    @Subscribe 
    public void newJobInQueue(JobPlacedInQueueEvent e) {
        if (e.getManager()==acqManager) {
            ((JobTableModel)jobTable.getModel()).setJobs(acqManager.getJobs());
            if (!scheduleCountDown.isRunning()) {                
                scheduleCountDown.restart();
            }
        }
    }
    
    @Subscribe
    public void allJobsCompleted(AllJobsCompletedEvent e) {
        if (e.getManager()==acqManager) {
            scheduleCountDown.stop();
            final JDialog frame=this;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    jobTable.getSelectionModel().clearSelection();
                    stopAllButton.setEnabled(false);
                    skipButton.setEnabled(false);
                }
            });
        }
    }
    
    @Subscribe
    public void currentJobChanged(final JobSwitchEvent e) {
        final JobTableModel model=(JobTableModel)jobTable.getModel();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (e==null) {
                    jobTable.getSelectionModel().clearSelection();
                } else {
                    if (selectCurrentJob(e.getCurrentJob().getId())) {
                        //job is in list and selection was succesful
                        stopAllButton.setEnabled(true);
                        skipButton.setEnabled(true);
                    } 
                }
            }    
        });
    }
    
    @Subscribe
    public void jobStatusUpdated(final JobStatusChangedEvent e) {
        final JobTableModel model=(JobTableModel)jobTable.getModel();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                model.updateStatus(e.getJob());
            }
        });
    }

    @Subscribe
    public void imageStored(final JobImageStoredEvent e) {
        final JobTableModel model=(JobTableModel)jobTable.getModel();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                model.updateStatus(e.getJob());
            }
        });
    }
    
    public boolean selectCurrentJob(final String jobId) {
        JobTableModel model=(JobTableModel)jobTable.getModel();
        int row=jobTable.convertRowIndexToView(model.getRowForJob(jobId));
        if (row!=-1) {
            jobTable.getSelectionModel().setSelectionInterval(row, row);
        }
        return row!=-1;
    }
}
