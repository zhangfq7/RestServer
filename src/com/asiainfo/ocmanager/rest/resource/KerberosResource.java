package com.asiainfo.ocmanager.rest.resource;

import com.asiainfo.ocmanager.rest.resource.downloadUtils.DownloadResult;
import com.asiainfo.ocmanager.rest.resource.downloadUtils.GetFile;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Map;

/**
 * Created by zhangfq on 2017/8/1.
 */

@Path("/kerberos")
public class KerberosResource {

    @GET
    @Path("getkeytab/{tenantId}/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKeytab(@PathParam("tenantId")String tenantId,@PathParam("username")String username){
        Map map = GetFile.getFile(tenantId,username);
        DownloadResult dr = (DownloadResult)map.get("res");
        Response.ResponseBuilder responseBuilder = Response.ok();
        responseBuilder.type("application/json");
        Response response = responseBuilder.entity(dr).build();
        return response;
    }

    @GET
    @Path("getFile/{tenantId}/{username}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getFile(@PathParam("tenantId")String tenantId,@PathParam("username")String username){
        Map map = GetFile.getFile(tenantId,username);
        Response.ResponseBuilder responseBuilder = Response.ok(map.get("file"));
        responseBuilder.type("applicatoin/octet-stream");
        responseBuilder.header("Content-Disposition", "attachment; filename="+((File)map.get("file")).getName());
        responseBuilder.header("Content-Length", Long.toString(((File)map.get("file")).length()));
        Response response = responseBuilder.build();
        return response;
    }
}
