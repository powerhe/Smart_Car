#include <jni.h>

#include <android/log.h>
#include <chrono>

#include "../common/JniHelpers.h"

#include "../common/CallbackSupport.h"

// for debug
#include "../common/Logger.h"
#include "../finder/PathFinder.h"
#include "NeighborSelectorEnum.h"
#include "HeuristicEnum.h"
#include "../common/Timer.h"

#define DEBUG_BENCHMARK

DECLARE_CACHED_CLASS(callbackClass,
                     "com/lenovo/newdevice/tangocar/path/finder/CCallback");

DECLARE_CACHED_CLASS(pathSinkClass,
                     "com/lenovo/newdevice/tangocar/path/finder/PathSink");

DECLARE_CACHED_METHOD_ID(callbackClass, getNodeTypeID, "getNodeType",
                         "(II)I");

DECLARE_CACHED_METHOD_ID(callbackClass, onCompleteID, "onComplete",
                         "(I)V");

DECLARE_CACHED_METHOD_ID(callbackClass, onStartID, "onStart",
                         "()V");

DECLARE_CACHED_METHOD_ID(pathSinkClass, receiveID, "receive",
                         "(II)V");

extern "C"
void
Java_com_lenovo_newdevice_tangocar_path_finder_FinderC_nativeFind(
        JNIEnv *jenv, jclass thisClass,
        jint startX, jint startY,
        jint goalX, jint goalY,
        jint heuristic, jint neighborSelector,
        jboolean dijkstra, jboolean shuffle,
        jint maxStep, jlong timeout,
        jobject callbackObject, jobject sinkObject) {

    Timer::begin("Method transaction");

    JNIEnv *jenvNoAttach = getJNIEnvFromJavaVM(global_jvm, NEVER_ATTACH);

    RETURN_AND_THROW_IF_NULL(callbackObject, "Null callback in param");
    RETURN_AND_THROW_IF_NULL(sinkObject, "Null sink in param");

    // get the method and cache it.
    GET_CACHED_METHOD_ID(jenvNoAttach, getNodeTypeID);
    RETURN_AND_THROW_IF_NULL(getNodeTypeID, "getting getNodeTypeID");

    GET_CACHED_METHOD_ID(jenvNoAttach, receiveID);
    RETURN_AND_THROW_IF_NULL(receiveID, "getting receiveID");

    GET_CACHED_METHOD_ID(jenvNoAttach, onStartID);
    RETURN_AND_THROW_IF_NULL(onStartID, "getting onStartID");

    GET_CACHED_METHOD_ID(jenvNoAttach, onCompleteID);
    RETURN_AND_THROW_IF_NULL(onCompleteID, "getting onCompleteID");

    // Start call
    jenvNoAttach->CallVoidMethod(callbackObject, onStartID);

    // Now calling Finder:
    PathFinder finder;

    finder.setStart(new WeightedPoint(startX, startY));
    finder.setGoal(new WeightedPoint(goalX, goalY));

    finder.setNeighborSelector(buildSelector(neighborSelector));
    finder.setHeuristic(buildHeuristic(heuristic));

    finder.setTimeout(timeout);

    finder.setShuffle(shuffle);
    finder.setDijkstra(dijkstra);
    finder.setMaxStep(maxStep);

    finder.setChecker([&](const WeightedPoint &pos) -> bool {
        int res = jenvNoAttach->CallIntMethod(callbackObject, getNodeTypeID, pos.getX(),
                                              pos.getY());
        if (res == EXPIRE_CANCELED) {
            finder.cancel();
        }
        return res == TRAVELABLE;
    });

    Timer::end("Method transaction");
    Timer::begin("Solve");

    FinderStatus status = finder.resolve();

    Timer::end("Solve");
    Timer::begin("Send");

    if (status == COMPLETED_FOUND) {

        std::vector<WeightedPoint *> path = finder.getPath();

        int size = (int) path.size();

        LOGD("Path size:%d", size);

        for (WeightedPoint *p:path) {
            jenvNoAttach->CallVoidMethod(sinkObject, receiveID, p->getX(), p->getY());
        }
    }

    // Complete call
    LOGI("Complete with status:%d", status);
    jenvNoAttach->CallVoidMethod(callbackObject, onCompleteID, status);

    Timer::end("Send");

    LOGD("%s", Timer::summery().c_str());
    Timer::clear();
}