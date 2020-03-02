// -*- coding: utf-8 -*-

// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

// Licensed under the Amazon Software License (the "License"). You may not use this file except in
// compliance with the License. A copy of the License is located at

//    http://aws.amazon.com/asl/

// or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
// language governing permissions and limitations under the License.

using System;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace AlexaSmartHomeLambda
{
    public class AlexaResponse
    {
        private JObject _jEvent = JObject.Parse("{}");
        
        private JObject _jResponse = JObject.Parse("{}");
        private JObject _jHeader = JObject.Parse("{}");
        private JObject _jEndpoint = JObject.Parse("{}");
        private JObject _jPayload = JObject.Parse("{}");
        
        public AlexaResponse() : this("Alexa", "Response")
        {
        }


        public AlexaResponse(string nameSpace, string name, string endpointId = "INVALID", string token = "INVALID", string correlationToken = null)
        {   
            _jHeader.Add("namespace", CheckValue(nameSpace, "Alexa"));
            _jHeader.Add("name", CheckValue(name, "Response"));

            _jHeader.Add("messageId", System.Guid.NewGuid());
            _jHeader.Add("payloadVersion", "3");

            if (correlationToken != null) {
                _jHeader.Add("correlationToken", CheckValue(correlationToken, "INVALID"));
            }
            
            JObject jScope = JObject.Parse("{}");
            jScope.Add("type", "BearerToken");
            jScope.Add("token", CheckValue(token, "INVALID"));

            _jEndpoint.Add("scope", jScope);
            _jEndpoint.Add("endpointId", CheckValue(endpointId, "INVALID"));
            
            _jEvent.Add("header", JToken.FromObject(_jHeader));

            // No endpoint in an AcceptGrant or Discover request
            if (name != "AcceptGrant.Response" && name != "Discover.Response")
            {
                _jEvent.Add("endpoint", JToken.FromObject(_jEndpoint));
            }

            _jEvent.Add("payload", JToken.FromObject(_jPayload));
            
            _jResponse.Add("event", JToken.FromObject(_jEvent));
        }


        public void AddContextProperty(string namespaceValue = "Alexa.EndpointHealth", string name = "connectivity", string value = "{}", int uncertaintyInMilliseconds = 0)
        {
            if (_jResponse["context"] == null)
            {
                JObject jContext = new JObject();
                JArray properties = new JArray();
                properties.Add(JObject.Parse(CreateContextProperty(namespaceValue, name, value, uncertaintyInMilliseconds)));
                jContext.Add("properties", properties);
                _jResponse.Add("context", jContext);   
            }
            else
            {
                JObject jContext = JObject.FromObject(_jResponse["context"]);
                JArray properties = new JArray();
                properties.Add(JObject.Parse(CreateContextProperty(namespaceValue, name, value, uncertaintyInMilliseconds)));
                jContext.Add("properties", properties);
                _jResponse["context"] = jContext;  
            }
        }


        public string CreateContextProperty(string namespaceValue = "Alexa.EndpointHealth", string name = "connectivity", string value = "{}", int uncertaintyInMilliseconds = 0)
        {
            String valueObject;
            try
            {
                valueObject = JObject.Parse(value).ToString();
            }
            catch (JsonReaderException)
            {
                valueObject = value;
            }

            JObject jProperty = new JObject();
            jProperty.Add("namespace", namespaceValue);
            jProperty.Add("name", name);
            jProperty.Add("value", valueObject);
            jProperty.Add("timeOfSample", DateTime.UtcNow);
            jProperty.Add("uncertaintyInMilliseconds", uncertaintyInMilliseconds);

            return jProperty.ToString();
        }

        
        public void AddCookie(string key, string value)
        {
            JObject jEndpoint = JObject.FromObject(_jResponse["event"]["endpoint"]);
            JToken cookie = jEndpoint["cookie"];

            if (cookie != null)
            {
                jEndpoint["cookie"][key] = value;
            }
            else
            {
                string cookieString = string.Format("{{\"{0}\": \"{1}\"}}", key, value);
                jEndpoint.Add("cookie", JToken.Parse(cookieString));                
            }
            
            _jResponse["event"]["endpoint"] = jEndpoint;
        }


        public void AddPayloadEndpoint(string endpointId, string capabilities)
        {
            JObject jPayload = JObject.FromObject(_jResponse["event"]["payload"]);

            bool hasEndpoints = jPayload.TryGetValue("endpoints", out var endpointsToken);

            if (hasEndpoints)
            {
                JArray endpoints = JArray.FromObject(endpointsToken);
                endpoints.Add(JObject.Parse(CreatePayloadEndpoint(endpointId, capabilities)));
                jPayload["endpoints"] = endpoints;
            }
            else
            {
                JArray endpoints = new JArray();
                endpoints.Add(JObject.Parse(CreatePayloadEndpoint(endpointId, capabilities)));
                jPayload.Add("endpoints", endpoints);
            }

            _jResponse["event"]["payload"] = jPayload;
        }


        public string CreatePayloadEndpoint(string endpointId, string capabilities, string cookie = null){
            JObject jEndpoint = new JObject();
            jEndpoint.Add("capabilities", JArray.Parse(capabilities));
            jEndpoint.Add("description", "Sample Endpoint Description");
            JArray displayCategories = new JArray();
            displayCategories.Add("LIGHT");
            jEndpoint.Add("displayCategories", displayCategories);
            jEndpoint.Add("endpointId", endpointId);
            //endpoint.Add("endpointId", "endpoint_" + new Random().Next(0, 999999).ToString("D6"));
            jEndpoint.Add("friendlyName", "Sample Switch");
            jEndpoint.Add("manufacturerName", "MARA.ai, LLC");

            if (cookie != null)
                jEndpoint.Add("cookie", JObject.Parse(cookie));

            return jEndpoint.ToString();
        }


        public void AddPayloadEndpointCapability(string endpointId, string capability)
        {
            JObject jPayload = JObject.FromObject(_jResponse["event"]["payload"]);

            bool hasEndpoints = jPayload.TryGetValue("endpoints", out var endpointsToken);

            if (hasEndpoints)
            {
                JArray endpoints = JArray.FromObject(endpointsToken);
                if (endpoints.HasValues)
                {
                    foreach (JObject endpoint in endpoints.Children())
                    {
                        if (endpoint["endpointId"].ToString() == endpointId)
                        {
                            JArray.FromObject(endpoint["capabilities"]).Add(JToken.Parse(capability));
//                            Response["event"]["endpoint"] = endpoint;
                        }
                    }
                }
            }
            else
            {
                JArray endpoints = new JArray();
                jPayload.Add("endpoints", endpoints);
                _jResponse["event"]["payload"] = jPayload;
            }            
        }

        
        private string CheckValue(string value, string defaultValue)
        {
            if (String.IsNullOrEmpty(value))
                return defaultValue;
            
            return value;
        }


        public string CreatePayloadEndpointCapability(string type="AlexaInterface", string interfaceValue="Alexa", string version="3", string properties=null)
        {
            JObject jCapability = new JObject();
            jCapability.Add("type", type);
            jCapability.Add("interface", interfaceValue);
            jCapability.Add("version", version);

            if (properties != null)
                jCapability.Add("properties", JObject.Parse(properties));

            //jCapability.Add("proactivelyReported", true);
            //jCapability.Add("retrievable", true);

            return jCapability.ToString();
        }


        public void SetPayload(string payload)
        {
            _jResponse["event"]["payload"] = JObject.Parse(payload);
        }


        public override string ToString()
        {
            return _jResponse.ToString();
        }
    }
}
