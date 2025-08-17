import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { Upload, FileText, Brain, TrendingUp, Search, Plus, FolderOpen, Zap, BarChart3, Clock, CheckCircle, MessageCircle, Send, User, Bot } from 'lucide-react';
import { Button } from './components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './components/ui/tabs';
import { Input } from './components/ui/input';
import { Badge } from './components/ui/badge';
import { Separator } from './components/ui/separator';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from './components/ui/dialog';
import { Label } from './components/ui/label';
import { Textarea } from './components/ui/textarea';
import './App.css';

const API_BASE_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8001';

function App() {
  const [documents, setDocuments] = useState([]);
  const [insights, setInsights] = useState([]);
  const [projects, setProjects] = useState([]);
  const [qaHistory, setQaHistory] = useState([]);
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [searchTerm, setSearchTerm] = useState('');
  const [isCreateProjectOpen, setIsCreateProjectOpen] = useState(false);
  const [newProject, setNewProject] = useState({ name: '', description: '' });
  const [currentQuestion, setCurrentQuestion] = useState('');
  const [isAsking, setIsAsking] = useState(false);
  const chatEndRef = useRef(null);

  useEffect(() => {
    fetchInitialData();
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [qaHistory]);

  const fetchInitialData = async () => {
    try {
      await Promise.all([
        fetchDocuments(),
        fetchInsights(),
        fetchProjects(),
        fetchStats(),
        fetchQAHistory()
      ]);
    } catch (error) {
      console.error('Error fetching initial data:', error);
    }
  };

  const scrollToBottom = () => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const fetchDocuments = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/documents`);
      setDocuments(response.data.documents || []);
    } catch (error) {
      console.error('Error fetching documents:', error);
    }
  };

  const fetchInsights = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/insights`);
      setInsights(response.data.insights || []);
    } catch (error) {
      console.error('Error fetching insights:', error);
    }
  };

  const fetchProjects = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/projects`);
      setProjects(response.data.projects || []);
    } catch (error) {
      console.error('Error fetching projects:', error);
    }
  };

  const fetchStats = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/dashboard/stats`);
      setStats(response.data);
    } catch (error) {
      console.error('Error fetching stats:', error);
    }
  };

  const fetchQAHistory = async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/qa/history`);
      setQaHistory(response.data.qa_history || []);
    } catch (error) {
      console.error('Error fetching Q&A history:', error);
    }
  };

  const handleFileUpload = async (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setSelectedFile(file);
    setLoading(true);

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await axios.post(`${API_BASE_URL}/api/documents/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      console.log('Upload successful:', response.data);
      await fetchDocuments();
      await fetchStats();
      setSelectedFile(null);
      event.target.value = '';
    } catch (error) {
      console.error('Error uploading file:', error);
      alert('Error uploading file. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const createProject = async () => {
    if (!newProject.name.trim()) return;

    try {
      await axios.post(`${API_BASE_URL}/api/projects`, newProject);
      await fetchProjects();
      setNewProject({ name: '', description: '' });
      setIsCreateProjectOpen(false);
    } catch (error) {
      console.error('Error creating project:', error);
      alert('Error creating project. Please try again.');
    }
  };

  const getCategoryColor = (category) => {
    const colors = {
      technical: 'bg-blue-100 text-blue-800 border-blue-200',
      process: 'bg-green-100 text-green-800 border-green-200',
      business: 'bg-purple-100 text-purple-800 border-purple-200',
      general: 'bg-gray-100 text-gray-800 border-gray-200'
    };
    return colors[category] || colors.general;
  };

  const filteredInsights = insights.filter(insight =>
    insight.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    insight.content.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-3">
              <div className="flex items-center justify-center w-10 h-10 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-lg">
                <Brain className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-slate-900">Knowledge Navigator</h1>
                <p className="text-sm text-slate-600">AI-Powered Knowledge Management</p>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <div className="relative">
                <input
                  type="file"
                  accept=".pdf,.docx,.txt"
                  onChange={handleFileUpload}
                  className="hidden"
                  id="file-upload"
                  disabled={loading}
                />
                <label htmlFor="file-upload">
                  <Button 
                    className="cursor-pointer bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white"
                    disabled={loading}
                  >
                    <Upload className="w-4 h-4 mr-2" />
                    {loading ? 'Uploading...' : 'Upload Document'}
                  </Button>
                </label>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-8">
          <TabsList className="grid w-full grid-cols-4 lg:w-auto">
            <TabsTrigger value="dashboard" className="flex items-center space-x-2">
              <BarChart3 className="w-4 h-4" />
              <span>Dashboard</span>
            </TabsTrigger>
            <TabsTrigger value="documents" className="flex items-center space-x-2">
              <FileText className="w-4 h-4" />
              <span>Documents</span>
            </TabsTrigger>
            <TabsTrigger value="insights" className="flex items-center space-x-2">
              <Zap className="w-4 h-4" />
              <span>Insights</span>
            </TabsTrigger>
            <TabsTrigger value="projects" className="flex items-center space-x-2">
              <FolderOpen className="w-4 h-4" />
              <span>Projects</span>
            </TabsTrigger>
          </TabsList>

          {/* Dashboard Tab */}
          <TabsContent value="dashboard" className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <Card className="bg-gradient-to-br from-blue-50 to-blue-100 border-blue-200">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium text-blue-900">Total Documents</CardTitle>
                  <FileText className="h-4 w-4 text-blue-600" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-blue-900">{stats.total_documents || 0}</div>
                  <p className="text-xs text-blue-600 mt-1">
                    {stats.processed_documents || 0} processed
                  </p>
                </CardContent>
              </Card>

              <Card className="bg-gradient-to-br from-green-50 to-green-100 border-green-200">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium text-green-900">Insights Generated</CardTitle>
                  <Zap className="h-4 w-4 text-green-600" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-green-900">{stats.total_insights || 0}</div>
                  <p className="text-xs text-green-600 mt-1">
                    From {stats.processed_documents || 0} documents
                  </p>
                </CardContent>
              </Card>

              <Card className="bg-gradient-to-br from-purple-50 to-purple-100 border-purple-200">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium text-purple-900">Active Projects</CardTitle>
                  <FolderOpen className="h-4 w-4 text-purple-600" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-purple-900">{stats.total_projects || 0}</div>
                  <p className="text-xs text-purple-600 mt-1">
                    Knowledge repositories
                  </p>
                </CardContent>
              </Card>

              <Card className="bg-gradient-to-br from-orange-50 to-orange-100 border-orange-200">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium text-orange-900">Processing Rate</CardTitle>
                  <TrendingUp className="h-4 w-4 text-orange-600" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-orange-900">
                    {Math.round(stats.processing_rate || 0)}%
                  </div>
                  <p className="text-xs text-orange-600 mt-1">
                    Documents analyzed
                  </p>
                </CardContent>
              </Card>
            </div>

            {/* Recent Insights */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Zap className="w-5 h-5 text-blue-600" />
                  <span>Recent Insights</span>
                </CardTitle>
                <CardDescription>
                  Latest knowledge extracted from your documents
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {insights.slice(0, 5).map((insight) => (
                    <div key={insight.id} className="flex items-start space-x-3 p-4 rounded-lg bg-slate-50 border border-slate-200">
                      <div className="flex-shrink-0">
                        <div className="w-8 h-8 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-full flex items-center justify-center">
                          <Brain className="w-4 h-4 text-white" />
                        </div>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <h4 className="text-sm font-medium text-slate-900 truncate">{insight.title}</h4>
                          <Badge className={getCategoryColor(insight.category)}>
                            {insight.category}
                          </Badge>
                        </div>
                        <p className="text-sm text-slate-600 mt-1 line-clamp-2">{insight.content}</p>
                        <div className="flex items-center mt-2 text-xs text-slate-500">
                          <div className="flex items-center mr-4">
                            <div 
                              className="w-2 h-2 bg-gradient-to-r from-green-400 to-blue-500 rounded-full mr-1"
                              style={{ 
                                background: `linear-gradient(to right, #ef4444 ${(1-insight.importance_score)*100}%, #10b981 ${insight.importance_score*100}%)` 
                              }}
                            ></div>
                            <span>Score: {Math.round(insight.importance_score * 100)}%</span>
                          </div>
                          <Clock className="w-3 h-3 mr-1" />
                          <span>{new Date(insight.created_date).toLocaleDateString()}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Documents Tab */}
          <TabsContent value="documents" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <FileText className="w-5 h-5 text-blue-600" />
                  <span>Document Library</span>
                </CardTitle>
                <CardDescription>
                  Manage and track your uploaded documents
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid gap-4">
                  {documents.map((doc) => (
                    <div key={doc.id} className="flex items-center justify-between p-4 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors">
                      <div className="flex items-center space-x-3">
                        <div className="flex-shrink-0">
                          <FileText className="w-8 h-8 text-blue-600" />
                        </div>
                        <div>
                          <h4 className="text-sm font-medium text-slate-900">{doc.filename}</h4>
                          <p className="text-sm text-slate-600">
                            {doc.content_type} • Uploaded {new Date(doc.upload_date).toLocaleDateString()}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center space-x-3">
                        <Badge variant={doc.processed ? "default" : "secondary"} className="flex items-center space-x-1">
                          {doc.processed ? (
                            <>
                              <CheckCircle className="w-3 h-3" />
                              <span>Processed</span>
                            </>
                          ) : (
                            <>
                              <Clock className="w-3 h-3" />
                              <span>Processing</span>
                            </>
                          )}
                        </Badge>
                        <Badge variant="outline">{doc.insights_count} insights</Badge>
                      </div>
                    </div>
                  ))}
                  {documents.length === 0 && (
                    <div className="text-center py-12">
                      <FileText className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                      <h3 className="text-lg font-medium text-slate-900 mb-2">No documents yet</h3>
                      <p className="text-slate-600">Upload your first document to get started</p>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Insights Tab */}
          <TabsContent value="insights" className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Zap className="w-5 h-5 text-blue-600" />
                  <span>Knowledge Insights</span>
                </CardTitle>
                <CardDescription>
                  AI-extracted insights and recommendations from your documents
                </CardDescription>
                <div className="flex items-center space-x-2 mt-4">
                  <Search className="w-4 h-4 text-slate-400" />
                  <Input
                    placeholder="Search insights..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="max-w-sm"
                  />
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid gap-6">
                  {filteredInsights.map((insight) => (
                    <div key={insight.id} className="p-6 border border-slate-200 rounded-lg hover:shadow-md transition-shadow">
                      <div className="flex items-start justify-between mb-3">
                        <h3 className="text-lg font-semibold text-slate-900">{insight.title}</h3>
                        <div className="flex items-center space-x-2">
                          <Badge className={getCategoryColor(insight.category)}>
                            {insight.category}
                          </Badge>
                          <div className="flex items-center text-sm text-slate-500">
                            <div 
                              className="w-3 h-3 rounded-full mr-2"
                              style={{ 
                                background: `linear-gradient(to right, #ef4444 ${(1-insight.importance_score)*100}%, #10b981 ${insight.importance_score*100}%)` 
                              }}
                            ></div>
                            <span>{Math.round(insight.importance_score * 100)}%</span>
                          </div>
                        </div>
                      </div>
                      <p className="text-slate-700 leading-relaxed mb-4">{insight.content}</p>
                      <div className="flex items-center text-xs text-slate-500">
                        <Clock className="w-3 h-3 mr-1" />
                        <span>Created {new Date(insight.created_date).toLocaleDateString()}</span>
                      </div>
                    </div>
                  ))}
                  {filteredInsights.length === 0 && (
                    <div className="text-center py-12">
                      <Zap className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                      <h3 className="text-lg font-medium text-slate-900 mb-2">
                        {searchTerm ? 'No matching insights' : 'No insights yet'}
                      </h3>
                      <p className="text-slate-600">
                        {searchTerm ? 'Try different search terms' : 'Upload and process documents to generate insights'}
                      </p>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Projects Tab */}
          <TabsContent value="projects" className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold text-slate-900">Projects</h2>
                <p className="text-slate-600">Organize your knowledge by project</p>
              </div>
              <Dialog open={isCreateProjectOpen} onOpenChange={setIsCreateProjectOpen}>
                <DialogTrigger asChild>
                  <Button className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white">
                    <Plus className="w-4 h-4 mr-2" />
                    New Project
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>Create New Project</DialogTitle>
                    <DialogDescription>
                      Create a project to organize documents and insights
                    </DialogDescription>
                  </DialogHeader>
                  <div className="space-y-4">
                    <div>
                      <Label htmlFor="project-name">Project Name</Label>
                      <Input
                        id="project-name"
                        value={newProject.name}
                        onChange={(e) => setNewProject({...newProject, name: e.target.value})}
                        placeholder="Enter project name"
                      />
                    </div>
                    <div>
                      <Label htmlFor="project-description">Description</Label>
                      <Textarea
                        id="project-description"
                        value={newProject.description}
                        onChange={(e) => setNewProject({...newProject, description: e.target.value})}
                        placeholder="Enter project description"
                        rows={3}
                      />
                    </div>
                    <div className="flex justify-end space-x-2">
                      <Button variant="outline" onClick={() => setIsCreateProjectOpen(false)}>
                        Cancel
                      </Button>
                      <Button onClick={createProject} disabled={!newProject.name.trim()}>
                        Create Project
                      </Button>
                    </div>
                  </div>
                </DialogContent>
              </Dialog>
            </div>

            <div className="grid gap-6">
              {projects.map((project) => (
                <Card key={project.id} className="hover:shadow-lg transition-shadow">
                  <CardHeader>
                    <div className="flex items-start justify-between">
                      <div>
                        <CardTitle className="text-xl">{project.name}</CardTitle>
                        <CardDescription className="mt-2">{project.description}</CardDescription>
                      </div>
                      <Badge variant="outline" className="text-xs">
                        {project.documents?.length || 0} docs
                      </Badge>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-4">
                      <div className="flex items-center justify-between text-sm">
                        <span className="text-slate-600">Insights: {project.insights?.length || 0}</span>
                        <span className="text-slate-600">
                          Created: {new Date(project.created_date).toLocaleDateString()}
                        </span>
                      </div>
                      {project.insights && project.insights.length > 0 && (
                        <div className="space-y-2">
                          <h4 className="font-medium text-slate-900">Key Insights:</h4>
                          {project.insights.slice(0, 3).map((insight) => (
                            <div key={insight.id} className="flex items-center space-x-2 text-sm">
                              <div className="w-2 h-2 bg-blue-600 rounded-full flex-shrink-0"></div>
                              <span className="text-slate-700 truncate">{insight.title}</span>
                              <Badge className={getCategoryColor(insight.category)} variant="outline">
                                {insight.category}
                              </Badge>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))}
              {projects.length === 0 && (
                <Card>
                  <CardContent className="text-center py-12">
                    <FolderOpen className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-slate-900 mb-2">No projects yet</h3>
                    <p className="text-slate-600 mb-4">Create your first project to organize knowledge</p>
                    <Button 
                      onClick={() => setIsCreateProjectOpen(true)}
                      className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white"
                    >
                      <Plus className="w-4 h-4 mr-2" />
                      Create Project
                    </Button>
                  </CardContent>
                </Card>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  );
}

export default App;