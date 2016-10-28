## 2.1.1

This is a minor feature release (and probably should have been versioned as 2.2.0).

* [SERVER-1604](https://tickets.puppetlabs.com/browse/SERVER-1604) - During the
  archiving of a gatling report directory, we now gzip the `simulation.log` file
  to reduce disk usage for build history.

## 2.1.0

This is a minor feature and bugfix release. 

* [SERVER-1501](https://tickets.puppetlabs.com/browse/SERVER-1501) - Adds graph
  of used heap memory over the course of a given run, if `metrics.json` is
  available.
* [SERVER-1500](https://tickets.puppetlabs.com/browse/SERVER-1500) - Fixes broken
  links to Gatling reports from one screen.

## 2.0.0

This is a major feature release.  Provides initial support for Jenkins Pipeline
jobs.
