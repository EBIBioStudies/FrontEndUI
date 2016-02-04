/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.arrayexpress.utils.controller;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.SchedulerException;

import java.util.Map;

public interface IJobsController {
    public void addJob(String name, Class<? extends Job> c, Map<String, Object> dataMap, String group) throws SchedulerException;

    public void addJob(String name, Class<? extends Job> c, JobDetail jobDetail) throws SchedulerException;

    public void executeJob(String name, String group) throws SchedulerException;

    public void addJobListener(JobListener jl) throws SchedulerException;
}
