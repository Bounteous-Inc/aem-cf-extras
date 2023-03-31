# AEM CF Extras
Extensions to AEM Content Fragments, including GraphQL support and additional model fields.

> **Warning**
__EOL: This project is no longer maintained as much of it has been incorporated into the latest versions of AEM.  There may be some parts of it that remain useful as reference for customizing Content Fragments, however it is not suggested to install the project as is.__

## Custom Fields
Field | Description
------------ | -------------
**CFX ID** | Displays an auto generated UUID for a CF based on its path.
**CFX Parent Field** | A virtual reference to an attribute in a content fragment located in a parent folder.
**CFX Child Reference** | A reference to a set of child content fragments. An optional relative folder can be specified along with the desired model type to include.
**CFX Content Fragment Reference** | Similar to the out of box Content Reference field except it can be restricted to a particular model type. This comes into play with the GraphQL API.
**CFX Image Field** | Points to an image in the DAM and shows a thumbnail preview of the currently selected image.
**CFX Date Time** | Similar to the out of the box Date and Time field except you can set it to handle just date, just time, or date and time.
**CFX Tags** | Like the out of box Tags field except it allows the choice of single or multiple tag selection.
**CFX Multi line text** | Like the out of box Multi line text field except it allows for adding multiple text areas
**CFX Tab** | Allows for fields in a model to be broken up into separate tabs

## GraphQL ##
AEM CF Extras also supports a GraphQL implementation that can be used to query your content fragments without the client requiring any insight into your repository structure.
Queries are divided up by configuration folders under `/conf`.  Any configuration folder that has models under it is turned into a type.  
### Schemas ###
First, to get a schema showing what can be queried in your Content Fragments, use a request with no query.  
```
http://localhost:4503/services/cfx/graphql
```  
From the schema you can see what you can query.  An example schema might look like this.  
```
type Query {
  aemCfExtras: AemCfExtras
}

type AemCfExtras {
  companyById(id: ID): Company
  companyList(displayName: [String], headquarters: [String]): [Company]
  conferenceById(id: ID): Conference
  conferenceList(displayName: [String]): [Conference]
  divisionById(id: ID): Division
  divisionList(displayName: [String]): [Division]
  organizationById(id: ID): Organization
  organizationList(abbreviation: [String], commissioner: [String], displayName: [String], headquarters: [String]): [Organization]
  playerById(id: ID): Player
  playerList(displayName: [String], firstName: [String], lastName: [String], nickname: [String]): [Player]
  sportById(id: ID): Sport
  sportList(displayName: [String], equipment: [String]): [Sport]
  teamById(id: ID): Team
  teamList(city: [String], displayName: [String]): [Team]
}

type Sport {
  description: String
  displayName: String
  equipment: [String]
  id: ID
  organizations: [String]
  state: Tag
}
...
```
### Queries ###
Now you know what queries you can request.  An example might be something like this.  
```
http://localhost:4502/services/cfx/graphql?query={aemCfExtras {sportList {id displayName equipment} } }
```
And you will get a response looking something like this.
```
{
  "data": {
    "aemCfExtras": {
      "sportList": [
        {
          "id": "12588189-42b3-3abd-8b4c-8b307711a9a7",
          "displayName": "Baseball",
          "equipment": [
            "Baseball",
            "Bat",
            "Glove",
            "Bases"
          ]
        },
        {
          "id": "e8d0ba5f-ad87-3cae-8356-e02e5a158d12",
          "displayName": "Basketball",
          "equipment": [
            "Basketball",
            "Hoop"
          ]
        },
        {
          "id": "53f2d3f2-7f31-335c-adb4-539691322fc7",
          "displayName": "Football",
          "equipment": [
            "Football",
            "Football Helmet",
            "Pads"
          ]
        }
      ]
    }
  }
}
```
### List vs ById ###
You will notice that each of the types are queryable with either a `List` or `ById` suffix.  
A list query might bring back more than one result in an array.  Even if it only returns one result it is still wrapped in a single element array.  
If you have already queried the items you want and want to go back for more details on one you can use the `id` property with the `ById` suffix.  This will always return a single object, not an array.  

### Filtering ###
The GraphQL implementation in AEM CF Extras currently supports basic filtering on string properties.  For example, if I know the team I wanted was `Cubs` then I could query this.
```
{
  aemCfExtras {
    teamList(displayName: "Cubs") {
      id
      displayName
      logo
    }
  }
}
```
And it would give me back this result.
```
{
  "data": {
    "aemCfExtras": {
      "teamList": [
        {
          "id": "f0eca96a-42f9-3aa7-8372-08c4b2913861",
          "displayName": "Cubs",
          "logo": "/content/dam/aem-cf-extras/team-logos/Chicago_Cubs.svg"
        }
      ]
    }
  }
}
```
*NOTE: More complex filtering will be added in future versions.*

### GraphQL Playground ###
A third party querying tool called GraphQL Playground has been embedded in the AEM Touch UI under `Tools -> AEM CF Extras -> GraphQL Playground`.  
By default, it points to the AEM CF Extras GraphQL endpoint servlet on the same AEM instance.  
It comes with functioning `SCHEMA` and `DOCS` tabs to the right as well as an autocompletion query editor.  
*NOTE: Currently you must add `/services/cfx/graphql` to the list of excluded paths in the "Adobe Granite CSRF Filter" config.*  

### In The Works ###
* More custom fields
* Sling Model Injectors for each custom field type
* More complex query filter options
* Variations support
* Better caching
* Test coverage
* Code cleanup and documenting
