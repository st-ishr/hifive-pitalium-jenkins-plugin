package org.jenkinsci.plugins;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.*;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.tasks.test.TestObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**テスト実行後に呼び出される．*/
public class PitaPublisher extends TestDataPublisher{
    private final String resultPicsAddr;
    @DataBoundConstructor
    public PitaPublisher(String resultPicsAddr){
        this.resultPicsAddr=resultPicsAddr;
    }

    public static FilePath getAttachmentPath(Run<?, ?> build) {
        return new FilePath(new File(build.getRootDir().getAbsolutePath()))
                .child("pictures");
    }

    public static FilePath getAttachmentPath(FilePath root, String child) {
        FilePath dir = root;
        if (!StringUtils.isEmpty(child)) {
            dir = dir.child(TestObject.safe(child));
        }
        return dir;
    }

    @Override
    public Data contributeTestData(Run<?, ?> build, FilePath workspace, Launcher launcher,
                                   TaskListener listener, TestResult testResult) throws IOException,InterruptedException
    {
        final GetResultOutput obj=new GetResultOutput(build, workspace, launcher, listener, testResult,resultPicsAddr);
        HashMap<String,HashMap<String,HashMap<String,List<String>>>> pictures=obj.getPictures();

        if (pictures.isEmpty()) {
            return null;
        }

        return new Data(pictures);
    }

    //TODO 動作チェック：パスにString利用有り．
    public static class Data extends TestResultAction.Data {
        private HashMap<String,HashMap<String,HashMap<String,List<String>>>> pictures;

        public Data(HashMap<String,HashMap<String,HashMap<String,List<String>>>> pictures){
            this.pictures=pictures;
        }

        @Override
        @SuppressWarnings("deprecation")
        public List<TestAction> getTestAction(hudson.tasks.junit.TestObject t) {
            TestObject testObject = (TestObject) t;
            String packageName;
            String className;
            String testName;

            List<String> attachmentPaths=new ArrayList<String>();
            if (testObject instanceof CaseResult) {
                packageName = testObject.getParent().getParent().getName();
                className = packageName+"."+testObject.getParent().getName();
                testName = testObject.getName();
                for(String var:pictures.get(packageName).get(className).get(testName)){
                    attachmentPaths.add(packageName+"/"+className+"/"+testName+"/"+var);
                }
            } else if (testObject instanceof ClassResult) {
                packageName = testObject.getParent().getName();
                className = packageName+"."+testObject.getName();
                for(Map.Entry<String,List<String>> tstCase:pictures.get(packageName).get(className).entrySet()){
                    testName=tstCase.getKey();
                    for(String var:tstCase.getValue()){
                        attachmentPaths.add(packageName+"/"+className+"/"+testName+"/"+var);
                    }
                }
            } else if (testObject instanceof PackageResult) {
                packageName = testObject.getName();
                for(Map.Entry<String,HashMap<String,List<String>>> tstCls:pictures.get(packageName).entrySet()){
                    className=tstCls.getKey();
                    for(Map.Entry<String,List<String>> tstCase:tstCls.getValue().entrySet()){
                        testName=tstCase.getKey();
                        for(String var:tstCase.getValue()){
                            attachmentPaths.add(packageName+"/"+className+"/"+testName+"/"+var);
                        }
                    }
                }
            }else if (testObject instanceof TestResult) {
                for(Map.Entry<String,HashMap<String,HashMap<String,List<String>>>> tstPkg:pictures.entrySet()){
                    packageName=tstPkg.getKey();
                    for(Map.Entry<String,HashMap<String,List<String>>> tstCls:tstPkg.getValue().entrySet()){
                        className=tstCls.getKey();
                        for(Map.Entry<String,List<String>> tstCase:tstCls.getValue().entrySet()){
                            testName=tstCase.getKey();
                            for(String var:tstCase.getValue()){
                                attachmentPaths.add(packageName+"/"+className+"/"+testName+"/"+var);
                            }
                        }
                    }
                }
            }else{//EXCEPTION?
                return Collections.emptyList();
            }

            String fullName = "";

            FilePath root = getAttachmentPath(testObject.getRun());
            PitaTestAction action = new PitaTestAction(testObject,
                    getAttachmentPath(root, fullName), attachmentPaths);
            return Collections.<TestAction> singletonList(action);
        }
    }
    @Extension
    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Override
        public String getDisplayName() {
            return "Pitaliumプラグイン";
        }

    }
}
