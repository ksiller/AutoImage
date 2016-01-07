package autoimage.gui;

import autoimage.events.JobSwitchEvent;
import autoimage.Job;
import autoimage.JobManager;
import autoimage.events.JobStatusChangedEvent;
import autoimage.events.AllJobsCompletedEvent;
import autoimage.events.JobCreatedEvent;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.InvalidParameterException;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author Karsten Siller
 */
public class JobStatusWindow extends JDialog {
    
    private JobManager acqManager;
    private List<Job> jobs;
    private final JTable jobTable;
    private final JButton stopAllButton;
    private final JButton skipButton;
    
    public JobStatusWindow (Frame p, JobManager manager, List<Job> j) {
        super(p);
        if (manager==null) {
            throw new InvalidParameterException("Job Manager cannot be null");
        }
        acqManager=manager;
        jobs=j;
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
        jobTable = new JTable(new JobTableModel(jobs));
        jobTable.setEnabled(false);
        jobTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                }
            }
            
        });
        JScrollPane scrollPane=new JScrollPane(jobTable);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        springLayout.putConstraint(SpringLayout.NORTH, scrollPane, 10, SpringLayout.NORTH, getContentPane());     
        springLayout.putConstraint(SpringLayout.SOUTH, scrollPane, -10, SpringLayout.NORTH, buttonPanel);
        springLayout.putConstraint(SpringLayout.EAST, scrollPane, -10, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, scrollPane, +10, SpringLayout.WEST, getContentPane());
                 
        getContentPane().add(scrollPane);
        getContentPane().add(buttonPanel);
        pack();
        if (acqManager!=null) {
            selectCurrentJob(acqManager.getCurrentJob());
        }
    }    

    @Subscribe
    public void jobCreated(JobCreatedEvent e) {
        ((JobTableModel)jobTable.getModel()).updateTable();
    }
    
    @Subscribe
    public void allJobsCompleted(AllJobsCompletedEvent e) {
        final JDialog frame=this;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jobTable.getSelectionModel().clearSelection();
                frame.setTitle("Job Manager: Finished");
                stopAllButton.setEnabled(false);
                skipButton.setEnabled(false);
            }
        });        
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
                    selectCurrentJob(e.getCurrentJob());
                    stopAllButton.setEnabled(true);
                    skipButton.setEnabled(true);
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
                if (e.getJob().isRunning()) {
                    setTitle("Job Manager: Running job "+e.getJob().getId());                    
                } else if (Job.JobRunning(e.getOldStatus()) && Job.JobRunning(e.getNewStatus())) {
                    setTitle("Job Manager: No jobs running");                    
                }                
                model.updateStatus(e.getJob());
            }
        });
    }

    public void selectCurrentJob(Job currentJob) {
        JobTableModel model=(JobTableModel)jobTable.getModel();
        int row=jobTable.convertRowIndexToView(model.getRowForJob(currentJob));
        jobTable.getSelectionModel().setSelectionInterval(row, row);
    }
}
