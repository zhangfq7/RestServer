package com.asiainfo.ocmanager.rest.resource.quotaUtils;

import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by zhangfq on 2017/9/1.
 */
public class MyContentSummary {

    private static Logger logger = Logger.getLogger(HdfsUtil.class);

    public ContentSummary getContentSummary(Path f, FileSystem fs) throws IOException {
        FileStatus status = fs.getFileStatus(f);
        short num = status.getReplication();
//        logger.info("The replications is:"+num);
        if (status.isFile()) {
            // f is a file
            long length = status.getLen();
            if(num!=0){
                return new ContentSummary.Builder().length(length/(long)num).
                    fileCount(1).directoryCount(0).spaceConsumed(length).build();
            }
            return new ContentSummary.Builder().length(length).
                fileCount(1).directoryCount(0).spaceConsumed(length).build();
        }
        // f is a directory
        long[] summary = {0, 0, 1};
        for(FileStatus s : fs.listStatus(f)) {
            long length = s.getLen();
            if(num!=0){
                ContentSummary c = s.isDirectory() ? getContentSummary(s.getPath(),fs) :
                    new ContentSummary.Builder().length(length/(long)num).
                        fileCount(1).directoryCount(0).spaceConsumed(length).build();
                summary[0] += c.getLength();
                summary[1] += c.getFileCount();
                summary[2] += c.getDirectoryCount();
            }else{
                ContentSummary c = s.isDirectory() ? getContentSummary(s.getPath(),fs) :
                    new ContentSummary.Builder().length(length).
                        fileCount(1).directoryCount(0).spaceConsumed(length).build();
                summary[0] += c.getLength();
                summary[1] += c.getFileCount();
                summary[2] += c.getDirectoryCount();
            }
        }
        return new ContentSummary.Builder().length(summary[0]).
            fileCount(summary[1]).directoryCount(summary[2]).
            spaceConsumed(summary[0]).build();
    }

}
