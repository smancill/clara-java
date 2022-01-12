/*
 * SPDX-FileCopyrightText: © The Clara Framework Authors
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jlab.clara.std.services;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.jlab.clara.base.ClaraUtil;
import org.jlab.clara.engine.Engine;
import org.jlab.clara.engine.EngineData;
import org.jlab.clara.engine.EngineDataType;
import org.jlab.clara.util.EnvUtils;
import org.jlab.clara.util.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * A service to copy files between a mounted shared file-system and the local
 * files-system.
 */
public class DataManager implements Engine {

    private static final String NAME = "DataManager";

    private static final String CONF_INPUT_PATH = "input_path";
    private static final String CONF_OUTPUT_PATH = "output_path";
    private static final String CONF_STAGE_PATH = "stage_path";

    private static final String REQUEST_TYPE = "type";
    private static final String REQUEST_EXEC = "exec";
    private static final String REQUEST_QUERY = "query";

    private static final String REQUEST_ACTION = "action";
    private static final String REQUEST_FILENAME = "file";

    private static final String REQUEST_EXEC_STAGE = "stage_input";
    private static final String REQUEST_EXEC_REMOVE = "remove_input";
    private static final String REQUEST_EXEC_SAVE = "save_output";
    private static final String REQUEST_EXEC_CLEAR = "clear_stage";

    private static final String REQUEST_QUERY_CONFIG = "get_config";

    private static final String REQUEST_INPUT_FILE = "input_file";
    private static final String REQUEST_OUTPUT_FILE = "output_file";

    private final String baseDir;

    private volatile DirectoryPaths directoryPaths;
    private volatile String outputPrefix;

    /**
     * Creates a new data manager service.
     */
    public DataManager() {
        this(EnvUtils.claraHome());
    }

    /**
     * Creates a new data manager service.
     *
     * @param baseDir the parent for the data directories.
     */
    public DataManager(String baseDir) {
        this.baseDir = baseDir;
        reset();
    }

    /**
     * Configures the engine with the given input data.
     * The data should contain the following parameters:
     * <ol>
     * <li> {@code input_path}: path to the location of the input-data files.</li>
     * <li> {@code output_path}: destination path of the output-data file.</li>
     * <li> {@code staging_path} (optional): data-file staging location,
     *      that is also used by the orchestrator to configure RW services.</li>
     * </ol>
     * @param input JSON text containing the configuration parameters
     * @return paths or error
     */
    @Override
    public EngineData configure(EngineData input) {
        var output = new EngineData();
        var mimeType = input.getMimeType();
        if (mimeType.equalsIgnoreCase(EngineDataType.JSON.mimeType())) {
            var data = (String) input.getData();
            try {
                var config = new JSONObject(data);
                updateConfiguration(config);
                returnData(output, getConfiguration());
            } catch (IllegalArgumentException e) {
                System.err.printf("%s config: %s%n", NAME, e.getMessage());
                ServiceUtils.setError(output, e.getMessage());
            } catch (JSONException e) {
                var error = "invalid request: " + data;
                System.err.printf("%s config: %s%n", NAME, error);
                ServiceUtils.setError(output, error);
            }
        } else {
            var error = "wrong mime-type: " + mimeType;
            System.err.printf("%s config: %s%n", NAME, error);
            ServiceUtils.setError(output, error);
        }
        return output;
    }

    private void updateConfiguration(JSONObject config) {
        var paths = new DirectoryPaths(config);
        System.out.printf("%s service: input path set to %s%n", NAME, paths.inputPath);
        System.out.printf("%s service: output path set to %s%n", NAME, paths.outputPath);
        if (config.has(CONF_STAGE_PATH)) {
            System.out.printf("%s service: stage path set to %s%n", NAME, paths.stagePath);
        }
        directoryPaths = paths;
    }

    JSONObject getConfiguration() {
        return directoryPaths.getConfiguration();
    }

    /**
     * Executes the engine with the given input data.
     * Accepts a JSON text with an action and an input file name.
     *
     * Current version assumes that there is a CLAS12 convention
     * that reconstructed/output file name is constructed as:
     * {@code "out_" + input_file_name}
     * <ul>
     * <li>
     * If the <em>action</em> is {@code stage_input} the input file will be
     * copied to the staging directory. The full paths to the input and output files
     * in the staging directory will be returned, so the orchestrator can use them to
     * configure the reader and writer services.
     * <li>
     * If the <em>action</em> is {@code remove_input} the input file will be
     * removed from the staging directory.
     * <li>
     * If the <em>action</em> is {@code save_output} the output file will be
     * saved to the final location and removed from the staging directory.
     * </ul>
     *
     * The data can also be the string {@code get_config}, in which case a JSON text
     * with the configured paths will be returned.
     *
     * @param input JSON text
     * @return paths, file names or error
     */
    @Override
    public EngineData execute(EngineData input) {
        var output = new EngineData();
        var mimeType = input.getMimeType();
        if (mimeType.equalsIgnoreCase(EngineDataType.JSON.mimeType())) {
            var data = (String) input.getData();
            try {
                var request = new JSONObject(data);
                var type = request.getString(REQUEST_TYPE);
                switch (type) {
                    case REQUEST_EXEC -> runAction(request, output);
                    case REQUEST_QUERY -> runQuery(request, output);
                    default -> ServiceUtils.setError(output, "invalid %s value: %s", REQUEST_TYPE, type);
                }
            } catch (IllegalArgumentException e) {
                ServiceUtils.setError(output, e.getMessage());
            } catch (JSONException e) {
                ServiceUtils.setError(output, "invalid request: " + data);
            } catch (Exception e) {
                ServiceUtils.setError(output, "unexpected problem:%n%s", ClaraUtil.reportException(e));
            }
        } else {
            ServiceUtils.setError(output, "wrong mimetype: " + mimeType);
        }
        return output;
    }

    private void runAction(JSONObject request, EngineData output) {
        var action = request.getString(REQUEST_ACTION);
        switch (action) {
            case REQUEST_EXEC_STAGE -> stageInputFile(getFiles(request), output);
            case REQUEST_EXEC_REMOVE -> removeStagedInputFile(getFiles(request), output);
            case REQUEST_EXEC_SAVE -> saveOutputFile(getFiles(request), output);
            case REQUEST_EXEC_CLEAR -> clearStageDir(output);
            default -> ServiceUtils.setError(output, "invalid %s value: %s", REQUEST_ACTION, action);
        }
    }

    private void runQuery(JSONObject request, EngineData output) {
        var action = request.getString(REQUEST_ACTION);
        if (action.equals(REQUEST_QUERY_CONFIG)) {
            returnData(output, getConfiguration());
        } else {
            ServiceUtils.setError(output, "invalid %s value: %s", REQUEST_ACTION, action);
        }
    }

    private void stageInputFile(FilePaths files, EngineData output) {
        var stagePath = FileUtils.getParent(files.stagedInputFile);
        var outputStream = new ByteArrayOutputStream();
        try {
            FileUtils.createDirectories(stagePath);

            var cmdLine = new CommandLine("cp");
            cmdLine.addArgument(files.inputFile.toString());
            cmdLine.addArgument(files.stagedInputFile.toString());

            var executor = new DefaultExecutor();
            var streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            executor.execute(cmdLine);
            System.out.printf("%s service: input file '%s' copied to '%s'%n",
                              NAME, files.inputFile, stagePath);
            returnFilePaths(output, files);

        } catch (ExecuteException e) {
            ServiceUtils.setError(output,
                    "could not complete request: " + outputStream.toString().trim());
        } catch (IOException e) {
            ServiceUtils.setError(output, "could not complete request: " + e.getMessage());
        }
    }

    private void removeStagedInputFile(FilePaths files, EngineData output) {
        var outputStream = new ByteArrayOutputStream();
        try {
            var cmdLine = new CommandLine("rm");
            cmdLine.addArgument(files.stagedInputFile.toString());

            var executor = new DefaultExecutor();
            var streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            executor.execute(cmdLine);
            System.out.printf("%s service: staged input file %s removed%n",
                              NAME, files.stagedInputFile);
            returnFilePaths(output, files);

        } catch (ExecuteException e) {
            ServiceUtils.setError(output,
                    "could not complete request: " + outputStream.toString().trim());
        } catch (IOException e) {
            ServiceUtils.setError(output, "could not complete request: " + e.getMessage());
        }
    }

    private void saveOutputFile(FilePaths files, EngineData output) {
        var outputPath = FileUtils.getParent(files.outputFile);
        var outputStream = new ByteArrayOutputStream();
        try {
            FileUtils.createDirectories(outputPath);

            var cmdLine = new CommandLine("mv");
            cmdLine.addArgument(files.stagedOutputFile.toString());
            cmdLine.addArgument(files.outputFile.toString());

            var executor = new DefaultExecutor();
            var streamHandler = new PumpStreamHandler(outputStream);
            executor.setStreamHandler(streamHandler);

            executor.execute(cmdLine);
            System.out.printf("%s service: output file '%s' saved to '%s'%n",
                              NAME, files.stagedOutputFile, outputPath);
            returnFilePaths(output, files);

        } catch (ExecuteException e) {
            ServiceUtils.setError(output,
                    "could not complete request: " + outputStream.toString().trim());
        } catch (IOException e) {
            ServiceUtils.setError(output, "could not complete request: " + e.getMessage());
        }
    }

    private void clearStageDir(EngineData output) {
        var stagePath = directoryPaths.stagePath;
        try {
            FileUtils.deleteFileTree(stagePath);
            System.out.printf("%s service: removed stage directory '%s'%n", NAME, stagePath);
            returnData(output, getConfiguration());
        } catch (IOException e) {
            ServiceUtils.setError(output, "could not complete request: " + e.getMessage());
        }
    }

    private FilePaths getFiles(JSONObject request) {
        var inputFileName = request.getString(REQUEST_FILENAME);
        return new FilePaths(directoryPaths, outputPrefix, inputFileName);
    }

    private void returnFilePaths(EngineData output, FilePaths files) {
        var fileNames = new JSONObject();
        fileNames.put(REQUEST_INPUT_FILE, files.stagedInputFile.toString());
        fileNames.put(REQUEST_OUTPUT_FILE, files.stagedOutputFile.toString());
        returnData(output, fileNames);
    }

    private void returnData(EngineData output, JSONObject data) {
        output.setData(EngineDataType.JSON.mimeType(), data.toString());
    }


    private static class DirectoryPaths {

        private final Path inputPath;
        private final Path outputPath;
        private final Path stagePath;

        DirectoryPaths(String baseDir) {
            inputPath = Paths.get(baseDir, "data", "input");
            outputPath = Paths.get(baseDir, "data", "output");
            stagePath = Paths.get("/scratch");
        }

        DirectoryPaths(JSONObject data) {
            inputPath = getPath(data, CONF_INPUT_PATH, "input");
            outputPath = getPath(data, CONF_OUTPUT_PATH, "output");
            stagePath = data.has(CONF_STAGE_PATH)
                    ? getPath(data, CONF_STAGE_PATH, "stage")
                    : Paths.get("/scratch");
        }

        JSONObject getConfiguration() {
            var config = new JSONObject();
            config.put(CONF_INPUT_PATH, inputPath.toString());
            config.put(CONF_OUTPUT_PATH, outputPath.toString());
            config.put(CONF_STAGE_PATH, stagePath.toString());
            return config;
        }
    }


    private static class FilePaths {

        private final Path stagedOutputFile;
        private final Path outputFile;
        private final Path stagedInputFile;
        private final Path inputFile;

        FilePaths(DirectoryPaths dirPaths, String outputPrefix, String inputFileName) {
            if (inputFileName.isEmpty()) {
                throw new IllegalArgumentException("empty input file name");
            }
            String outputFileName = outputPrefix + inputFileName;

            inputFile = dirPaths.inputPath.resolve(inputFileName);
            outputFile = dirPaths.outputPath.resolve(outputFileName);

            Path resolvedFileName = inputFile.getFileName();
            if (resolvedFileName == null || !inputFileName.equals(resolvedFileName.toString())) {
                throw new IllegalArgumentException("invalid input file name: " + inputFileName);
            }

            stagedInputFile = dirPaths.stagePath.resolve(inputFileName);
            stagedOutputFile = dirPaths.stagePath.resolve(outputFileName);
        }
    }


    private static Path getPath(JSONObject data, String key, String type) {
        var path = Paths.get(data.getString(key));
        if (path.toString().isEmpty()) {
            throw new IllegalArgumentException("empty " + type + " path");
        }
        if (!path.isAbsolute()) {
            var error = String.format("%s path %s is not absolute", type, path);
            throw new IllegalArgumentException(error);
        }
        if (Files.exists(path) && !Files.isDirectory(path)) {
            var error = String.format("%s path %s exists but not a directory", type, path);
            throw new IllegalArgumentException(error);
        }
        return path;
    }


    @Override
    public EngineData executeGroup(Set<EngineData> inputs) {
        return null;
    }

    @Override
    public Set<EngineDataType> getInputDataTypes() {
        return ClaraUtil.buildDataTypes(EngineDataType.JSON);
    }

    @Override
    public Set<EngineDataType> getOutputDataTypes() {
        return ClaraUtil.buildDataTypes(EngineDataType.JSON);
    }

    @Override
    public Set<String> getStates() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Copy files from/to local disk.";
    }

    @Override
    public String getVersion() {
        return "0.10";
    }

    @Override
    public String getAuthor() {
        return "Sebastián Mancilla  <smancill@jlab.org>";
    }

    @Override
    public void reset() {
        directoryPaths = new DirectoryPaths(baseDir);
        outputPrefix = "out_";
    }

    @Override
    public void destroy() {
        // nothing
    }
}
