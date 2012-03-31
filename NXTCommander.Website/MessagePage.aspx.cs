using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using System.Text;

public partial class MessagePage : System.Web.UI.Page
{
    public List<string> History { get; set; }

    public string LastCommand { get; set; }

    protected void Page_Load(object sender, EventArgs e)
    {
        History = ChatWebService.ChatHistory;
        StringBuilder sb = new StringBuilder();
                            
        //History.ForEach(line => sb.AppendLine(line));

        //ChatHistoryList.Text = sb.ToString();

        if(History.Any())
            ChatHistoryList.Text = History.Last();
    }
}