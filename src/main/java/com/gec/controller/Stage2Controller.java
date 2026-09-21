package com.gec.controller;

import com.gec.service.Stage2HiveService;
import com.gec.service.Stage2OdsImportService;
import com.gec.service.Stage2PipelineService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/Stage2")
public class Stage2Controller {
    private final Stage2HiveService hiveService;
    private final Stage2OdsImportService odsImportService;
    private final Stage2PipelineService pipelineService;

    public Stage2Controller(Stage2HiveService hiveService, Stage2OdsImportService odsImportService, Stage2PipelineService pipelineService) {
        this.hiveService = hiveService;
        this.odsImportService = odsImportService;
        this.pipelineService = pipelineService;
    }

    @GetMapping("/hiveHealth")
    public R hiveHealth() throws Exception {
        return R.ok(hiveService.health());
    }

    @GetMapping("/warehouseStatus")
    public R warehouseStatus() throws Exception { return R.ok(hiveService.warehouseStatus()); }

    @PostMapping("/bootstrapHive")
    public R bootstrapHive() throws Exception {
        return R.ok(hiveService.bootstrap());
    }

    @PostMapping("/importOdsSamples")
    public R importOdsSamples(String batchDate) throws Exception {
        return R.ok(odsImportService.importSamples(batchDate));
    }

    @PostMapping("/buildBehaviorAndReview")
    public R buildBehaviorAndReview(String batchDate) throws Exception {
        if (batchDate == null || batchDate.trim().isEmpty()) throw new IllegalArgumentException("batchDate 不能为空");
        return R.ok(hiveService.buildBehaviorAndReview(batchDate.trim()));
    }

    @PostMapping("/buildSales")
    public R buildSales(String batchDate) throws Exception {
        if (batchDate == null || batchDate.trim().isEmpty()) throw new IllegalArgumentException("batchDate 不能为空");
        return R.ok(hiveService.buildSales(batchDate.trim()));
    }

    @PostMapping("/buildService")
    public R buildService(String batchDate) throws Exception {
        if (batchDate == null || batchDate.trim().isEmpty()) throw new IllegalArgumentException("batchDate 不能为空");
        return R.ok(hiveService.buildService(batchDate.trim()));
    }

    @PostMapping("/runAll")
    public R runAll(String batchDate) throws Exception { return R.ok(pipelineService.run(validDate(batchDate))); }

    @PostMapping("/retryStep")
    public R retryStep(String batchDate,String step) throws Exception {
        String date=validDate(batchDate), target=step==null?"":step.trim(); Object result;
        if("behavior-review".equals(target)) result=hiveService.buildBehaviorAndReview(date);
        else if("sales".equals(target)) result=hiveService.buildSales(date);
        else if("service".equals(target)) result=hiveService.buildService(date);
        else if("all".equals(target)) result=pipelineService.run(date);
        else throw new IllegalArgumentException("step 仅支持 behavior-review、sales、service、all");
        Map<String,Object> out=new LinkedHashMap<>(); out.put("step",target); out.put("batchDate",date); out.put("result",result); return R.ok(out);
    }

    private String validDate(String value) { if(value==null||value.trim().isEmpty())throw new IllegalArgumentException("batchDate 不能为空"); return LocalDate.parse(value.trim()).toString(); }

    @ExceptionHandler(Exception.class)
    public void errors(Exception e, HttpServletResponse response) {
        e.printStackTrace();
        R.err(e).write(response);
    }
}
