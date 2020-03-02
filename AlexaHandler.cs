// -*- coding: utf-8 -*-

// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

// Licensed under the Amazon Software License (the "License"). You may not use this file except in
// compliance with the License. A copy of the License is located at

//    http://aws.amazon.com/asl/

// or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
// language governing permissions and limitations under the License.

using System;
using System.IO;
using System.Text;
using System.Threading.Tasks;
using Amazon;
using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.DocumentModel;
using Amazon.Lambda.Core;
using Newtonsoft.Json.Linq;

namespace AlexaSmartHomeLambda
{
    public class AlexaHandler
    {   
        public Stream Handler(Stream inputStream, ILambdaContext context)
        {
            StreamReader reader = new StreamReader(inputStream);
            string request  = reader.ReadToEnd();

            //Console.Out.WriteLine("Request:");
            //Console.Out.WriteLine(request);
            
            if (context != null)
            {
                context.Logger.Log("Request:");
                context.Logger.Log(request);
            }

            AlexaResponse alexaResponse;
            
            JObject jRequest = JObject.Parse(request);

            string nameSpace = jRequest["directive"]["header"]["namespace"].Value<string>();
            string name = jRequest["directive"]["header"]["name"].Value<string>();

            switch (nameSpace)
            {
                //case "Alexa":
                //    switch (name)
                //    {
                //        case "ReportState":
                //            string requestedEndpointId = jRequest["directive"]["endpoint"]["endpointId"].Value<string>();

                //            if (context != null)
                //                context.Logger.Log($"Alexa::ReportState Request: {requestedEndpointId}");


                //            alexaResponse = new AlexaResponse("Alexa", "StateReport");
                //            break;

                //        default:
                //            if(context != null)
                //                context.Logger.Log("INVALID name in Namespace Alexa.");

                //            alexaResponse = new AlexaResponse();
                //            break;
                //    }

                //    break;

                case "Alexa.Authorization":
                    if (context != null)
                        context.Logger.Log("Alexa.Authorization Request");

                    alexaResponse = new AlexaResponse("Alexa.Authorization", "AcceptGrant.Response");
                    break;
                
                case "Alexa.Discovery":
                    if (context != null)
                        context.Logger.Log("Alexa.Discovery Request");
                    
                    alexaResponse = new AlexaResponse("Alexa.Discovery", "Discover.Response", "endpoint-001");

                    JObject jCapabilityAlexa = JObject.Parse(alexaResponse.CreatePayloadEndpointCapability());
            
                    JObject jPropertyPowerstate = new JObject();
                    jPropertyPowerstate.Add("name", "powerState");
                    JObject jCapabilityAlexaPowerController = JObject.Parse(alexaResponse.CreatePayloadEndpointCapability("AlexaInterface", "Alexa.PowerController", "3", jPropertyPowerstate.ToString()));
            
                    JArray capabilities = new JArray();
                    capabilities.Add(jCapabilityAlexa);
                    capabilities.Add(jCapabilityAlexaPowerController);
            
                    alexaResponse.AddPayloadEndpoint("endpoint-001", capabilities.ToString());                    
                    break;

                case "Alexa.PowerController":
                    if (context != null)
                        context.Logger.Log("Alexa.PowerController Request");

                    string correlationToken = jRequest["directive"]["header"]["correlationToken"].Value<string>();
                    string endpointId = jRequest["directive"]["endpoint"]["endpointId"].Value<string>();                    
                    
                    string state = (name == "TurnOff") ? "OFF" : "ON"; 
                    
                    bool result = StoreDeviceState(endpointId, "powerState", state);

                    if (result)
                    {
                        alexaResponse = new AlexaResponse("Alexa", "Response", endpointId, "INVALID", correlationToken);
                        alexaResponse.AddContextProperty("Alexa.PowerController", "powerState", state, 200);
                    }
                    else
                    {
                        JObject jPayloadError = new JObject();
                        jPayloadError.Add("type", "ENDPOINT_UNREACHABLE");
                        jPayloadError.Add("message", "There was an error setting the device state.");
                        alexaResponse = new AlexaResponse("Alexa", "ErrorResponse");
                        alexaResponse.SetPayload(jPayloadError.ToString());
                    }
                    break;
                    
                default:
                    if (context != null)
                        context.Logger.Log("INVALID Namespace");

                    alexaResponse = new AlexaResponse();
                    break;
            }
            
            string response = alexaResponse.ToString();

            if (context != null)
            {
                context.Logger.Log("Response:");
                context.Logger.Log(response);
            }

            return new MemoryStream(Encoding.UTF8.GetBytes(response));
        }


        public bool StoreDeviceState(String endpointId, String state, String value)
        {
            String attributeValue = state + "Value";
            
            AmazonDynamoDBClient dynamoClient = new AmazonDynamoDBClient(RegionEndpoint.USEast1);
            Table table = Table.LoadTable(dynamoClient, "SampleSmartHome");

            Document item = new Document();
            item["ItemId"] = endpointId;
            item[attributeValue] = value;

            Task<Document> updateTask = table.UpdateItemAsync(item);
            updateTask.Wait();

            if (updateTask.Status == TaskStatus.RanToCompletion)
                return true;
            
            return false;
        }
        
    }
}
