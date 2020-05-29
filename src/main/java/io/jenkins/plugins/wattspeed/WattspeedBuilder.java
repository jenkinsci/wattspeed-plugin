package io.jenkins.plugins.wattspeed;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.bind.JavaScriptMethod;

public class WattspeedBuilder extends Builder implements SimpleBuildStep
{
  private static final Logger LOGGER = Logger.getLogger(WattspeedBuilder.class.getName());
  public static final String WATTSPEED_ENDPOINT = "https://api.wattspeed.com/";

  private final Secret token;
  protected final int projects;

  @DataBoundConstructor
  public WattspeedBuilder(Secret token, int projects)
  {
    this.token = token;
    this.projects = projects;
  }

  public String getToken()
  {
    return this.token.getPlainText();
  }

  public int getProject_id()
  {
    return this.projects;
  }

  public static JSONArray FetchProjects(Secret token) throws Exception
  {
    String response = DescriptorImpl.getPostResponse(WATTSPEED_ENDPOINT + "listwebpages", token.getPlainText(), false);
    if (JSONObject.fromObject(response).getInt("ok") == 1)
    {
      String projects = JSONObject.fromObject(response).getJSONObject("body").getString("webpages");
      return JSONArray.fromObject(projects);
    } else {
      return null;
    }
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
  {
    try
    {
      JSONArray projectsArray = this.FetchProjects(this.token);
      for (int i = 0; i < projectsArray.size(); i++)
      {
        String projectToken = null;
        if(projectsArray.getJSONObject(i).has("token"))
          projectToken = projectsArray.getJSONObject(i).getString("token");
        if(projectsArray.getJSONObject(i).getInt("id") == this.projects && projectToken != null)
        {
          listener.getLogger().println("Creating Wattspeed snapshot for " + projectsArray.getJSONObject(i).getString("url"));
          DescriptorImpl.getPostResponse(WATTSPEED_ENDPOINT + "update?token=" + URLEncoder.encode(projectToken, "UTF-8"), null, true);
          listener.getLogger().println("Snapshot successfully created");
          break;
        }
      }
    }
    catch (Exception e)
    {
      listener.getLogger().println("There was an error when trying to generate the Wattspeed snapshot");
      System.out.println(e.getMessage());
    }
  }

  @Symbol("wattspeed")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
  {
    private Secret token;
    private JSONArray projectsArray;
    private boolean validToken = false;
    private boolean hasProjects = true;

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass)
    {
      return true;
    }

    @Override
    public String getDisplayName()
    {
      return Messages.WattspeedBuilder_DescriptorImpl_DisplayName();
    }

    public FormValidation doCheckToken(@QueryParameter String value)
    {
      this.token = Secret.fromString(value);

      if (value.length() == 0)
        this.projectsArray = null;

      return FormValidation.ok();
    }

    public static String getPostResponse(String apiURL, String token, boolean isGetRequest) throws Exception
    {
      URL url = new URL(apiURL);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();

      if(!isGetRequest)
        con.setRequestMethod("POST");
      else
        con.setRequestMethod("GET");

      con.setRequestProperty("Content-Type", "application/json; utf-8");
      con.setRequestProperty("Accept", "application/json");
      con.setDoOutput(true);

      JSONObject json = new JSONObject();
      JSONObject jsonToken = new JSONObject();
      jsonToken.put("token", token);
      json.put("params", jsonToken);

      if(!isGetRequest)
      {
        try (OutputStream os = con.getOutputStream())
        {
          byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
          os.write(input, 0, input.length);
        }
      }

      StringBuilder response = new StringBuilder();
      try (BufferedReader br = new BufferedReader(
          new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)))
      {
        String responseLine = null;
        while ((responseLine = br.readLine()) != null)
        {
          response.append(responseLine.trim());
        }
      }

      return response.toString();
    }

    public void getProjects() throws Exception
    {
      if(this.token == null || this.token.getPlainText().length() == 0)
      {
        FormValidation.ok();
        return;
      }

      this.projectsArray = WattspeedBuilder.FetchProjects(this.token);
      if (this.projectsArray != null)
      {
        if(this.projectsArray.size() == 0) {
          this.validToken = true;
          this.hasProjects = false;
          FormValidation.warning("No webpages found in your Wattspeed account");
        } else {
          this.hasProjects = true;
          this.validToken = true;
          FormValidation.ok();
        }
      }
      else
      {
        this.validToken = false;
        this.projectsArray = null;
        FormValidation.error("Provided token is invalid");
      }
    }

    public ListBoxModel doFillProjectsItems(@QueryParameter String token)
    {
      ListBoxModel items = new ListBoxModel();
      if (this.projectsArray != null)
      {
        for (int i = 0; i < this.projectsArray.size(); i++)
        {
          if(this.projectsArray.getJSONObject(i).has("token")) {
            String device = this.projectsArray.getJSONObject(i).getString("device");
            device = device.substring(0, 1).toUpperCase() + device.substring(1);
            items.add(this.projectsArray.getJSONObject(i).getString("name") + " - " + device, this.projectsArray.getJSONObject(i).getInt("id") + "");
          }
        }
      }

      return items;
    }

    @JavaScriptMethod
    public int getProjectsArray(String token) {
      if(token != null) {
        this.token = Secret.fromString(token);
      }

      try
      {
        this.getProjects();
      }
      catch (Exception e)
      {
        return 0;
      }

      if(!this.hasProjects)
        return -2;

      if(!validToken)
        return -1;

      if(this.projectsArray == null)
        return 0;

      return this.projectsArray.size();
    }
  }

}
