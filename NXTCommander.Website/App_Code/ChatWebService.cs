using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Services;

/// <summary>
/// Summary description for ChatWebService
/// </summary>
[WebService(Namespace = "http://tempuri.org/")]
[WebServiceBinding(ConformsTo = WsiProfiles.BasicProfile1_1)]
// To allow this Web Service to be called from script, using ASP.NET AJAX, uncomment the following line. 
[System.Web.Script.Services.ScriptService]
public class ChatWebService : System.Web.Services.WebService
{
    /// <summary>
    /// A simple list of string to store the chat history in a static member
    /// </summary>
    public static List<string> ChatHistory = new List<string>();
    
    /// <summary>
    /// A configurable timeout (milliseconds)
    /// </summary>
    private const int TIMEOUT = 120000;

    /// <summary>
    /// Waits for new incoming messages
    /// </summary>
    /// <param name="count">Current count of messages on the calling client</param>
    /// <returns>True if some new message arrived, False if timeout was reached</returns>
    [WebMethod]
    public bool PollForNewMessages(int count)
    {
        DateTime start = DateTime.Now;
        while ((DateTime.Now - start).TotalMilliseconds < TIMEOUT)
        {
            if (ChatHistory.Count > count)
            {
                return true;
            }
            System.Threading.Thread.Sleep(500);
        }
        return false;
    }

    /// <summary>
    /// Add a new message to the chat history
    /// </summary>
    /// <param name="message">Message to be added</param>
    [WebMethod]
    public void SendMessage(string message)
    {
        lock (ChatHistory)
        {
            ChatHistory.Add(message);
        }
    }
}
