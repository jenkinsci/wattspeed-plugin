Wattspeed plugin for Jenkins
=========================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/kubernetes.svg)](https://plugins.jenkins.io/kubernetes)

Wattspeed helps you monitor and improve web pages in‌ ‌order‌ ‌to‌ ‌give‌ ‌your‌ site‌ ‌a‌ ‌speed‌ ‌bump.

---

This plugin will let you triggers a website snapshot generation in your [Wattspeed](https://www.wattspeed.com/) account, from a Jenkins project.

# Setup

### Prerequisites

* API Token
  
    To get the token, log into your [Wattspeed](https://www.wattspeed.com/signin) account, then head forward to the
    [profile](https://www.wattspeed.com/profile) page.
  
  ![](E:\wattadmin\plugins\jenkins\images\profile.png)

### Generating a snapshot

* Add Wattspeed as a build step in the Build section of your Jenkins project.
  ![image](images/build-step.png)
* Paste your token, then select a webpage.
  ![image](images/projects.png)