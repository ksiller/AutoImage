/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package autoimage.gui;

import autoimage.Job;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Karsten
 */
public class JobTableModel extends AbstractTableModel {

    public final String[] COLUMN_NAMES = new String[]{"Job", "Status", "Scheduled Time", "Delay [s]"};
    private List<Job> jobs;
    private final static DecimalFormat NUMBER_FORMAT = new DecimalFormat("###,###,##0.000");
    
    public JobTableModel(List<Job> j) {
        jobs=j;
    }
    
    public void setJobs(List<Job> j) {
        jobs=j;
        fireTableDataChanged();
    } 
            
    @Override
    public int getRowCount() {
        if (jobs!=null) {
            return jobs.size();
        } else
            return 0;
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int colIdx) {
        return COLUMN_NAMES[colIdx];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (jobs!=null && rowIndex < jobs.size()) {
            Job job=jobs.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return job.getId();
                case 1:
                    return job.getStatus();
                case 2: {
                    Calendar cal= new GregorianCalendar();
                    cal.setTimeInMillis(job.getScheduledTimeMS());
                    return cal.getTime().toString();
                    }
                case 3: {
                    if (job.getActualTimeMS()<0) {
                        return "";
                    } else {
                        return NUMBER_FORMAT.format(0.001d*(job.getActualTimeMS()-job.getScheduledTimeMS()));
                    }
                }
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    public void updateStatus(Job job) {
        int row=jobs.indexOf(job);
        if (row>=0) {
            this.fireTableRowsUpdated(row, row);
        }
    }

    public Job get(int row) {
        if (row>=0 && row < jobs.size()) {
            return jobs.get(row);
        } else {
            return null;
        }
    }
    
    public int getRowForJob(String id) {
        if (jobs!=null) {
            for (int i=0; i<jobs.size(); i++) {
                if (jobs.get(i).getId().equals(id)) {
                    return i;
                }    
            }
        } 
        return -1;
    }
    
    public void updateTable() {
        this.fireTableDataChanged();
    }
    
}
