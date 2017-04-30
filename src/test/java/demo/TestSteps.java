package demo;

import com.tiandi.mongo.testcase.TestCase;
import com.tiandi.service.FaultInjectionInfoService;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class TestSteps extends AbstractDefs {
    @Autowired
    private FaultInjectionInfoService faultInjectionInfoService;

    private String faultNodeId;

    @Given("^Generate testCase for \"([^\"]*)\"$")
    public void generateTestCase(String id) {
        faultNodeId = id;
    }

    @Then("^save to \"([^\"]*)\"$")
    public void finish(String filename) {
        TestCase tc = faultInjectionInfoService.generateTestCase(faultNodeId);
        try{
            faultInjectionInfoService.createTestCaseFile(tc,filename);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
