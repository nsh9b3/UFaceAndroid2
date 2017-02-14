package com.stuff.nsh9b3.ufaceandroid;

/**
 * Created by nick on 11/22/16.
 */

public class WebService
{
    public String serviceName;
    public String serviceAddress;
    public String userName;
    public int userIndex;

    public WebService(String serviceName, String serviceAddress, String userName, int userIndex)
    {
        this.serviceName = serviceName;
        this.serviceAddress = serviceAddress;
        this.userName = userName;
        this.userIndex = userIndex;
    }
}
