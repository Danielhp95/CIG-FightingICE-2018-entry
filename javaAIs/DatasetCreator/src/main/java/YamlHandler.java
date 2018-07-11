import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlHandler {

    final Logger logger = LoggerFactory.getLogger(YamlHandler.class);

    private String datasetPath;
    private final String METADATAFILEPATH;

    public YamlHandler(String datasetPath) {
       this.datasetPath = datasetPath;
       this.METADATAFILEPATH =  datasetPath + ".meta.yaml";
    }

    public Map findDatasetMetadata() {
        if (!exists(METADATAFILEPATH)) {
            logger.info("Metadata file NOT FOUND. Creating new metadatafile at directory: " + METADATAFILEPATH);
            return createMetaDataFile(METADATAFILEPATH);
        }
        return openYamlFile(METADATAFILEPATH);
}

    private boolean exists(String filePath) {
        return new File(filePath).isFile();
    }

    private Map createMetaDataFile(String filePath) {
        Map<String, Object> skeleton = new HashMap();
        skeleton.put("TotalFrames", 0);
        skeleton.put("ContestantAIs", new ArrayList<String>());
        writeMapToYamlFile(filePath, skeleton);
        return skeleton;
    }

    public Map addContestantToMetadata(Map metadataFile, String contestantAIName) {
        Map newContestantStatistics = new HashMap();
        newContestantStatistics.put("Rounds", 0);
        newContestantStatistics.put("Frames", 0);

        Map newContestant = new HashMap();
        newContestant.put(contestantAIName, newContestantStatistics);

        ArrayList contestantAIs = (ArrayList) metadataFile.get("ContestantAIs");
        contestantAIs.add(newContestant);
        metadataFile.put("ContestantAIs", contestantAIs);
        return metadataFile;
    }

    public Map openYamlFile(String filePath) {
        Yaml yaml = new Yaml();
        Map yamlFile = null;
        try {
            yamlFile = (Map) yaml.load(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return yamlFile;
    }

    private void writeMapToYamlFile(String filePath, Map content) {
        logger.info("Writting to file {} with content {}", filePath, content.toString());
        try {
            Yaml yaml = new Yaml();
            FileWriter newFile = new FileWriter(filePath);
            newFile.append(yaml.dump(content));
            newFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateDatasetMetadata(Map metadataFile, String contestantAIName, int roundProcessedFrames) {
        metadataFile = updateContestantIndependantMetadata(metadataFile, roundProcessedFrames);
        metadataFile = updateContestantDependantMetadata(metadataFile, contestantAIName, roundProcessedFrames);

        writeMapToYamlFile(METADATAFILEPATH, metadataFile);
    }

    private Map updateContestantIndependantMetadata(Map metadataFile, int roundProcessedFrames) {
        int previousFrames = (int) metadataFile.get("TotalFrames");
        metadataFile.put("TotalFrames", previousFrames + roundProcessedFrames);
        return metadataFile;
    }

    // TODO clean this mess up!
    private Map updateContestantDependantMetadata(Map metadataFile, String contestantAIName, int framesProcessedThisRound) {
        ArrayList<Map> contestantAIs = (ArrayList<Map>) metadataFile.get("ContestantAIs");

        Map contestantStatistics = findContestantStatisticsFromContestantList(contestantAIs, contestantAIName);

        contestantStatistics.put("Rounds", 1 + (Integer) contestantStatistics.get("Rounds"));
        contestantStatistics.put("Frames", framesProcessedThisRound + (Integer) contestantStatistics.get("Frames"));

        for (Map contestant : contestantAIs) {
            if (contestant.keySet().contains(contestantAIName)) {
                contestant.put(contestantAIName, contestantStatistics);
            }
        }
        metadataFile.put("ContestantAIs", contestantAIs);
        return metadataFile;
    }

    private Map findContestantStatisticsFromContestantList(List<Map> contestantList, String targetContestantName) {
       for (Map contestant : contestantList)  {
          if (contestant.keySet().contains(targetContestantName)) {
              return (Map) contestant.get(targetContestantName);
          }
       }
       logger.error("Could not find metadata information for AI: {}. This information must be present.", targetContestantName);
       return null;
    }

    // TODO clean mess (maybe find index , with -1 if didnt find it?)
    public boolean doesContestantExist(Map metaDataFile, String contestantAIName) {
        ArrayList<Map> contestantAIs = (ArrayList<Map>) metaDataFile.get("ContestantAIs");
        for (Map contestant : contestantAIs) {
            if (contestant.keySet().contains(contestantAIName)) {
                return true;
            }
        }
        return false;
    }

    public Integer findRoundNumberForContestant(Map metaDataFile, String targetContestantName) {
        ArrayList<Map> contestantAIs = (ArrayList<Map>) metaDataFile.get("ContestantAIs");
        for (Map contestant : contestantAIs) {
            if (contestant.keySet().contains(targetContestantName)) {
                return (Integer) ((Map) contestant.get(targetContestantName)).get("Rounds");
            }
        }
        logger.error("Could not find metadata information for AI: {}. This information must be present.", targetContestantName);
        return null;
    }
}
