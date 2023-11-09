package com.github.manu156.pinpointintegration.window;

import com.github.manu156.pinpointintegration.common.constant.Constants;
import com.github.manu156.pinpointintegration.common.dto.MetaData;
import com.github.manu156.pinpointintegration.common.dto.TransactionMetaData;
import com.github.manu156.pinpointintegration.common.exception.InputValidationException;
import com.github.manu156.pinpointintegration.common.exception.PluginException;
import com.github.manu156.pinpointintegration.common.runner.CancellationToken;
import com.github.manu156.pinpointintegration.common.runner.PooledThreadRun;
import com.github.manu156.pinpointintegration.common.runner.Runner;
import com.github.manu156.pinpointintegration.common.runner.SimpleWork;
import com.github.manu156.pinpointintegration.common.util.RestUtil;
import com.github.manu156.pinpointintegration.editor.RenderQueue;
import com.github.manu156.pinpointintegration.editor.util.RenderUtil;
import com.github.manu156.pinpointintegration.window.dto.TxnMetaDto;
import com.github.manu156.pinpointintegration.window.plot.GraphData;
import com.github.manu156.pinpointintegration.window.plot.ScatterPlot;
import com.google.gson.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.List;


public class CyanToolWindow {
    private static final Logger logger = Logger.getInstance(CyanToolWindow.class);
    private JPanel myToolWindowContent;
    private JComboBox<String> serverSelector;
    private JTabbedPane tabbedPane1;
    private JTextField pinpointUrlField;
    private JButton connectToServerBtn;
    private JLabel jStatus;
    private JTable table1;
    private JPanel trSpanel;
    private JPanel sct;
    private JRadioButton a5mRadioButton;
    private JRadioButton a20mRadioButton;
    private JRadioButton a1hRadioButton;
    private JRadioButton a12hRadioButton;
    private JRadioButton a3hRadioButton;
    private JRadioButton a6hRadioButton;
    private JRadioButton a1dRadioButton;
    private JTextField matchersZeroOrMoreTextField;
    private JButton fetchTxnBtn;
    private JButton refreshGraphButton;
    private JSlider xAxisLowerSlider;
    private JSlider yAxisLeftSlider;
    private JSlider xAxisUpperSlider;
    private JSlider yAxisRightSlider;
    private JButton loadSelectedButton;
    private JButton fetchMoreTransactionsButton;
    private JButton clearAllButton;
    private JButton clearLoadedButton;
    private JCheckBox apiFilterCheckBox;
    private JButton clearButton;
    private JLabel statusFieldLabel;
    private ScatterPlot graph;

    private DefaultTableModel tableModel;
    private Integer transactionTblOffset = 0;
    private List<String> spanIds = new ArrayList<>();
    private final BasicConfig basicConfig = BasicConfig.getInstance();
    private List<Long> txnListTs= new ArrayList<>();
    private Project project;

    private Map<String, String> appNameToServiceMap = new HashMap<>();

    ActionListener connectToServerActionListener = e -> {
        basicConfig.getState().url = pinpointUrlField.getText();
        connectToServerBtn.setEnabled(false);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            connectToPinpoint();
            connectToServerBtn.setEnabled(true);
        });
    };


    SimpleWork<Runner> fetchTxnWork = new SimpleWork<>(
            new PooledThreadRun(this::populateTransactionList),
            state -> {
                boolean isRunning = state.get().isRunning();
                statusFieldLabel.setText(isRunning ? "Status: Fetching transactions" : "Status: none");
                fetchTxnBtn.setText(isRunning ? "Cancel Fetch" : "Fetch Transactions");
                refreshGraphButton.setEnabled(!isRunning);
            }
    );
    ActionListener fetchTxnsActionListener = e -> fetchTxnWork.flip();

    SimpleWork<Runner> simpleWorkRefreshGraph = new SimpleWork<>(
            new PooledThreadRun(this::populateScatterPlot),
            state -> {
                boolean isRunning = state.get().isRunning();
                statusFieldLabel.setText(isRunning ? "Status: Refreshing Graph" : "Status: none");
                refreshGraphButton.setText(isRunning ? "Cancel Refresh" : "Refresh Graph");
                fetchTxnBtn.setEnabled(!isRunning);
            }
    );
    ActionListener refreshGraphActionListener = e -> simpleWorkRefreshGraph.flip();

    ActionListener loadSelectedActionListener = e -> ApplicationManager.getApplication().executeOnPooledThread(
            () -> {
                loadSelectedAPI();
            }
    );

    ChangeListener listener = e -> graph.MoveSelector(yAxisLeftSlider.getValue()/100D, yAxisRightSlider.getValue()/100D,
            xAxisUpperSlider.getValue()/100D, xAxisLowerSlider.getValue()/100D);

    ItemListener serverSelectorItemChangeListener = e -> {
        if (null != serverSelector.getSelectedItem())
            basicConfig.getState().server = serverSelector.getSelectedItem().toString();
    };

    ActionListener clearAllTxnListActionListener = e -> {
        tableModel.setRowCount(0);
        transactionTblOffset = 0;
        txnListTs.clear();
    };

    ActionListener clearLoadedTxnActionListener = e -> {
        RenderQueue rq = ApplicationManager.getApplication().getService(RenderQueue.class);
        rq.clear();
        rq.disposeAll();
    };

    public CyanToolWindow(@NotNull Project project) {
        this.project = project;
        setUpConfigurationTab();
        setUpTimeRangeTab();
        setUpTransactionsTab();
    }

    private void setUpTransactionsTab() {
        clearAllButton.addActionListener(clearAllTxnListActionListener);
        fetchMoreTransactionsButton.addActionListener(e -> fetchMoreTransactions());
        clearLoadedButton.addActionListener(clearLoadedTxnActionListener);
        loadSelectedButton.addActionListener(loadSelectedActionListener);
    }

    private void setUpTimeRangeTab() {
        apiFilterCheckBox.addItemListener(e -> matchersZeroOrMoreTextField.setEnabled(apiFilterCheckBox.isSelected()));
        clearButton.addActionListener(e -> matchersZeroOrMoreTextField.setText(""));

        refreshGraphButton.addActionListener(refreshGraphActionListener);
        fetchTxnBtn.addActionListener(fetchTxnsActionListener);

        xAxisLowerSlider.addChangeListener(listener);
        yAxisLeftSlider.addChangeListener(listener);
        xAxisUpperSlider.addChangeListener(listener);
        yAxisRightSlider.addChangeListener(listener);
    }

    private void setUpConfigurationTab() {
        loadLastConfiguration();

        connectToServerBtn.addActionListener(connectToServerActionListener);
        serverSelector.addItemListener(serverSelectorItemChangeListener);
    }

    private void loadLastConfiguration() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            pinpointUrlField.setText(basicConfig.getState().url);
            connectToPinpoint();

            if (serverSelector.isEnabled() && isServerInConfigInList()) {
                serverSelector.setSelectedItem(basicConfig.getState().server);
            }
        });
    }

    private boolean isServerInConfigInList() {
        for (int i = 0; i < serverSelector.getItemCount(); i++) {
            if (serverSelector.getItemAt(i).equals(basicConfig.getState().server)) {
                return true;
            }
        }
        return false;
    }


    private void fetchMoreTransactions() {
        if (null == tableModel) {
            logger.info("tableModel not yet initialized");
            return;
        }
        if (null == serverSelector.getSelectedItem()) {
            logger.info("no server selected");
            return;
        }
        List<TxnMetaDto> txnMetas = graph.getSelectedTxn();
        if (transactionTblOffset >= txnMetas.size()) {
            return;
        }

        Optional<TransactionMetaData> transactionMetaDataOptional = RestUtil.httpPost(
                getTransactionMetaDataUrl(),
                getTransactionMetaDataFormsBody(txnMetas, serverSelector.getSelectedItem().toString()),
                TransactionMetaData.class
        );

        transactionMetaDataOptional.ifPresent(transactionMetaData -> {
            for (MetaData metaData : transactionMetaData.metadata) {
                txnListTs.add(metaData.startTime);
                tableModel.addRow(getRowData(metaData));
                spanIds.add(metaData.spanId);
                table1.repaint();
            }
            transactionTblOffset += Math.min(txnMetas.size(), transactionTblOffset + 100);
        });
    }

    @NotNull
    private String getTransactionMetaDataUrl() {
        return pinpointUrlField.getText() + Constants.TRANSACTION_METADATA_PINPOINT;
    }

    @NotNull
    private static Object[] getRowData(MetaData metaData) {
        String formattedDateTime = Instant.ofEpochMilli(metaData.startTime).atZone(ZoneId.systemDefault())
                .toLocalDateTime().format(Constants.DATE_TIME_FORMAT_TRANSACTION_LIST);

        return new Object[] {
                formattedDateTime, metaData.application, metaData.elapsed, metaData.exception, metaData.agentId,
                metaData.endpoint, metaData.remoteAddr, metaData.traceId, metaData.agentName
        };
    }



    @NotNull
    private UrlEncodedFormEntity getTransactionMetaDataFormsBody(List<TxnMetaDto> txnMetas, String selectedServer) {
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("ApplicationName", selectedServer));

        for (int i = 0; i+transactionTblOffset < txnMetas.size() && i < 100; i++) {
            TxnMetaDto txnMeta = txnMetas.get(transactionTblOffset+i);
            formParams.add(new BasicNameValuePair("I" + i, txnMeta.i));
            formParams.add(new BasicNameValuePair("T" + i, txnMeta.t.toString()));
            formParams.add(new BasicNameValuePair("R" + i, txnMeta.r.toString()));
        }
        return new UrlEncodedFormEntity(formParams, Consts.UTF_8);
    }

    private void loadSelectedAPI() {
        RenderQueue rq = ApplicationManager.getApplication().getService(RenderQueue.class);
        int[] sr = table1.getSelectedRows();
        for (int row=0; row<table1.getSelectedRowCount(); row++) {
            String agentId = table1.getValueAt(sr[row], 4).toString();
            String spanId = spanIds.get(sr[row]);
            String traceId = table1.getValueAt(sr[row], 7).toString();
            String fTS = txnListTs.get(sr[row]).toString();

            try {
                String urlStrin = getTransactionInfoUrl(agentId, spanId, traceId, fTS);
                Optional<JsonObject> s = RestUtil.httpGet(urlStrin, JsonObject.class);
                s.ifPresent(t -> rq.add(t, agentId, spanId, traceId, fTS));
            } catch (PluginException e) {
                return;
            }
        }
        renderFun();
    }

    private String getTransactionInfoUrl(String agentId, String spanId, String traceId, String fTS) throws InputValidationException {
        try {
            URI uri = new URIBuilder(pinpointUrlField.getText() + "/transactionInfo.pinpoint")
                    .addParameter("agentId", agentId)
                    .addParameter("spanId", spanId)
                    .addParameter("traceId", traceId)
                    .addParameter("focusTimestamp", fTS)
                    .addParameter("useStatisticsAgentState", "false")
                    .build();
            return uri.toString();
        } catch (URISyntaxException e) {
            logger.info(e);
            throw new InputValidationException();
        }
    }

    private void renderFun() {
        ApplicationManager.getApplication().invokeLater(() -> {
            RenderQueue rq = ApplicationManager.getApplication().getService(RenderQueue.class);

            FileEditor[] fileEditors = FileEditorManager.getInstance(project).getAllEditors();
            List<JsonObject> js = rq.popNotRendered();
            if (null == js || js.isEmpty())
                return;
            RenderUtil.renderHelper(js, fileEditors, this.project);
        });
    }

    private void populateScatterPlot(CancellationToken refreshCancelToken) {
        logger.info("populateScatterPlot");
        if (0 == serverSelector.getItemCount())
            return;
        long offset = getInputOffset();
        try {
            long toTime = new Date().getTime();
            List<JsonObject> s;
            if (!matchersZeroOrMoreTextField.getText().isBlank() && !"/**/".equalsIgnoreCase(matchersZeroOrMoreTextField.getText()))
                s = callFilterMap(offset, toTime);
            else {
                s = callMap(offset, toTime);
            }
            GraphData graphData = new GraphData(s, toTime, offset);
            if (refreshCancelToken.isCancelled())
                return;
            logger.info("repopulating data");
            graph.populateNewData(graphData);
            logger.info("repopulated data");
        } catch (URISyntaxException | IOException e) {
            logger.info(e);
        }
    }

    private long getInputOffset() {
        long[] range = {5, 20, 60, 180, 360, 720, 1440};
        JRadioButton[] timeSelector = {a5mRadioButton, a20mRadioButton, a1hRadioButton, a3hRadioButton, a6hRadioButton,
                a12hRadioButton, a1dRadioButton};
        long offset = 60L*1000;
        for (int i=0; i< range.length; i++) {
            if (timeSelector[i].isSelected()) {
                offset *= range[i];
                break;
            }
        }
        return offset;
    }

    private List<JsonObject> callFilterMap(long offset, long toTime) throws URISyntaxException, IOException {
        List<JsonObject> res = new ArrayList<>();
        long currentTo = toTime;
        long currentFrom = toTime - offset;
        long originTo = currentTo;

        while (true) {
            JsonObject jsonObject = getJsonObjectFilter(currentFrom, currentTo, originTo);
            JsonObject scatterData = jsonObject.getAsJsonObject("applicationScatterData");
            String applicationName = serverSelector.getSelectedItem().toString();
            String serviceType = appNameToServiceMap.get(serverSelector.getSelectedItem().toString());
            JsonObject specificData = scatterData.getAsJsonObject(applicationName+"^"+serviceType);
            res.add(specificData);

            if (!jsonObject.has("lastFetchedTimestamp") ||
                    currentFrom == jsonObject.get("lastFetchedTimestamp").getAsLong()) {
                break;
            }
            currentTo = jsonObject.get("lastFetchedTimestamp").getAsLong()-1;
        }

        return res;
    }

    private JsonObject getJsonObjectFilter(long currentFrom, long currentTo, long originTo) throws URISyntaxException, IOException {
        String filterUrlBase64 = Base64.getEncoder().encodeToString(matchersZeroOrMoreTextField.getText().getBytes());
        String applicationName = serverSelector.getSelectedItem().toString();
        String serviceType = appNameToServiceMap.get(serverSelector.getSelectedItem().toString());
        String filter = "[{" +
                    "\"fa\":null," +
                    "\"fst\":null," +
                    "\"ta\":null," +
                    "\"tst\":null," +
                    "\"a\":\"" + applicationName + "\"," +
                    "\"st\":\"" + serviceType + "\"," +
                    "\"ie\":null," +
                    "\"rf\":0," +
                    "\"rt\":\"max\"," +
                    "\"url\":\"" + filterUrlBase64 + "\"" +
                "}]";
//        String filterUrlEncoded = Base64.getUrlEncoder().encodeToString(filter.getBytes());
        URI uri = new URIBuilder(pinpointUrlField.getText() + "/getFilteredServerMapDataMadeOfDotGroup.pinpoint")
                .addParameter("applicationName", applicationName)
                .addParameter("serviceTypeName", serviceType)
                .addParameter("from", String.valueOf(currentFrom))
                .addParameter("to", String.valueOf(currentTo))
                .addParameter("limit", "3000")
                .addParameter("xGroupUnit", getXGroupUnit(currentTo, currentFrom))
                .addParameter("yGroupUnit", getYGroupUnit())
                .addParameter("originTo", String.valueOf(originTo))
                .addParameter("calleeRange", "1")
                .addParameter("callerRange", "1")
                .addParameter("filter", filter)
//                .addParameter("hint", "true")
                .addParameter("v", "4")
                .addParameter("useStatisticsAgentState", "true")
                .build();

        String urlString = uri.toString();
        return callAPI(urlString);
    }

    private static JsonObject callAPI(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("accept", "application/json");
        InputStream responseStream = connection.getInputStream();
        String s = IOUtils.toString(responseStream, "UTF-8");
        logger.info(s);
        JsonObject jsonObject = new Gson().fromJson(s, JsonObject.class);
        return jsonObject;
    }

    private List<JsonObject> callMap(long offset, long toTime) throws URISyntaxException, IOException {
        List<JsonObject> res = new ArrayList<>();
        long currentTo = toTime;
        long currentFrom = toTime - offset;
        while(true) {
            URI uri = new URIBuilder(pinpointUrlField.getText() + "/getScatterData.pinpoint")
                    .addParameter("application", serverSelector.getSelectedItem().toString())
                    .addParameter("from", String.valueOf(currentFrom))
                    .addParameter("to", String.valueOf(currentTo))
                    .addParameter("limit", "3000")
                    .addParameter("xGroupUnit", getXGroupUnit(currentTo, currentFrom))
                    .addParameter("yGroupUnit", getYGroupUnit())
                    .addParameter("backwardDirection", "true")
                    .build();
            URL url = new URL(uri.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("accept", "application/json");
            InputStream responseStream = connection.getInputStream();
            String s = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
            logger.info(s);
            JsonObject jsonObject = new Gson().fromJson(s, JsonObject.class);
            res.add(jsonObject);
            if (!jsonObject.has("complete") ||
                    "true".equalsIgnoreCase(jsonObject.get("complete").getAsString()) ||
                    !jsonObject.has("resultFrom"))
                break;
            currentTo = jsonObject.get("resultFrom").getAsLong()-1;
        }
        return res;
    }

    @NotNull
    private String getYGroupUnit() {
        long yGroupUnit = Math.round(2500. / sct.getHeight());
        return yGroupUnit + "";
    }

    @NotNull
    private String getXGroupUnit(long currentTo, long currentFrom) {
        double timeDifferenceInMs = (double) currentTo - currentFrom;
        long xGroupUnit = Math.round(timeDifferenceInMs / sct.getWidth());
        return xGroupUnit + "";
    }

    private void populateTransactionList(CancellationToken fetchTxnCancelToken) {
        tableModel = new DefaultTableModel() {
            @Override
            @SuppressWarnings("rawtypes")
            public Class getColumnClass(int columnIndex) {
                for (int row = 0; row < getRowCount(); row++) {
                    Object o = getValueAt(row, columnIndex);
                    if (o != null)
                        return o.getClass();
                }
                return Object.class;
            }
        };
        txnListTs.clear();
        spanIds.clear();
        transactionTblOffset = 0;
        for (String columnName : Constants.TRANSACTIONS_LIST_COLUMN_HEADERS) {
            tableModel.addColumn(columnName);
        }
        table1.setModel(tableModel);
        List<TxnMetaDto> txnMetas = graph.getSelectedTxn();
        try {
            Optional<TransactionMetaData> transactionMetaDataOptional = RestUtil.httpPost(
                    getTransactionMetaDataUrl(),
                    getTransactionMetaDataFormsBody(txnMetas, serverSelector.getSelectedItem().toString()),
                    TransactionMetaData.class
            );

            if (fetchTxnCancelToken.isCancelled())
                return;

            transactionMetaDataOptional.ifPresent(transactionMetaData -> {
                for (MetaData metaData : transactionMetaData.metadata) {
                    txnListTs.add(metaData.startTime);
                    tableModel.addRow(getRowData(metaData));
                    spanIds.add(metaData.spanId);
                }
                // transactionTblOffset += Math.min(txnMetas.size(), transactionTblOffset + 100);
            });

            if (!fetchTxnCancelToken.isCancelled())
                tabbedPane1.setSelectedIndex(2);

            transactionTblOffset = Math.min(txnMetas.size(), transactionTblOffset+100);
        } catch (Exception e) {
            logger.info(e);
        }

    }


    private void connectToPinpoint() {
        try {
            serverSelector.removeAllItems();
            if (!StringUtils.isBlank(pinpointUrlField.getText())) {
                jStatus.setText("status: connecting");
                URL inputUrl = new URL(pinpointUrlField.getText());
                URL url = new URL(inputUrl.getProtocol(), inputUrl.getHost(), inputUrl.getPort(), "/applications.pinpoint");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("accept", "application/json");
                InputStream responseStream = connection.getInputStream();
                JsonArray jsonArray = new Gson().fromJson(IOUtils.toString(responseStream, StandardCharsets.UTF_8),
                        JsonArray.class);

                appNameToServiceMap.clear();
                for (JsonElement jsonElement : jsonArray) {
                    String appName = ((JsonObject) jsonElement).get("applicationName").getAsString();
                    String serviceType = ((JsonObject) jsonElement).get("serviceType").getAsString();
                    appNameToServiceMap.put(appName, serviceType);
                    serverSelector.addItem(appName);
                }

                serverSelector.setEnabled(true);
                jStatus.setText("status: connected");
                jStatus.setEnabled(true);
            }
        } catch (IOException e) {
            serverSelector.setEnabled(false);
            jStatus.setText("status: connection failed");
            logger.info(e);
        }  finally {
            jStatus.setEnabled(true);
        }
    }

    public JPanel getContent() {
        return myToolWindowContent;
    }


    private void createUIComponents() {
        // place custom component creation code here
        graph = new ScatterPlot();
        sct = graph;
    }
}
