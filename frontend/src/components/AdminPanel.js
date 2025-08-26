import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8001';

function AdminPanel({ user, onClose }) {
  const [activeTab, setActiveTab] = useState('users');
  const [users, setUsers] = useState([]);
  const [groups, setGroups] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [editForm, setEditForm] = useState({});

  useEffect(() => {
    if (user?.role === 'ADMIN') {
      loadAllData();
    }
  }, [user]);

  const loadAllData = async () => {
    setLoading(true);
    try {
      const [usersRes, groupsRes, docsRes] = await Promise.all([
        axios.get(`${API_BASE_URL}/api/admin/users`),
        axios.get(`${API_BASE_URL}/api/admin/groups`),
        axios.get(`${API_BASE_URL}/api/admin/documents`)
      ]);
      setUsers(usersRes.data);
      setGroups(groupsRes.data);
      setDocuments(docsRes.data);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };



  const createUser = async (userData) => {
    try {
      await axios.post(`${API_BASE_URL}/api/admin/users`, userData);
      loadAllData();
    } catch (error) {
      console.error('Error creating user:', error);
    }
  };

  const createGroup = async (groupData) => {
    try {
      await axios.post(`${API_BASE_URL}/api/admin/groups`, groupData);
      loadAllData();
    } catch (error) {
      console.error('Error creating group:', error);
    }
  };

  const deleteUser = async (id) => {
    try {
      await axios.delete(`${API_BASE_URL}/api/admin/users/${id}`);
      loadAllData();
    } catch (error) {
      console.error('Error deleting user:', error);
    }
  };

  const deleteGroup = async (id) => {
    try {
      await axios.delete(`${API_BASE_URL}/api/admin/groups/${id}`);
      loadAllData();
    } catch (error) {
      console.error('Error deleting group:', error);
    }
  };

  const updateUser = async (id, userData) => {
    try {
      await axios.put(`${API_BASE_URL}/api/admin/users/${id}`, userData);
      loadAllData();
      setEditingItem(null);
    } catch (error) {
      console.error('Error updating user:', error);
    }
  };

  const updateGroup = async (id, groupData) => {
    try {
      await axios.put(`${API_BASE_URL}/api/admin/groups/${id}`, groupData);
      loadAllData();
      setEditingItem(null);
    } catch (error) {
      console.error('Error updating group:', error);
    }
  };

  if (user?.role !== 'ADMIN') {
    return <div className="p-4">Access denied. Admin role required.</div>;
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-6xl w-full max-h-[90vh] overflow-hidden">
        <div className="flex justify-between items-center p-6 border-b">
          <h1 className="text-2xl font-bold text-gray-900">Admin Panel</h1>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
            ✕
          </button>
        </div>
        
        <div className="flex-1 overflow-hidden">
          <div className="border-b border-gray-200">
            <nav className="-mb-px flex space-x-8 px-6">
              {['users', 'groups', 'documents'].map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`py-4 px-6 border-b-2 font-medium text-sm ${
                    activeTab === tab
                      ? 'border-indigo-500 text-indigo-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700'
                  }`}
                >
                  {tab.charAt(0).toUpperCase() + tab.slice(1)}
                </button>
              ))}
            </nav>
          </div>

          <div className="p-6 overflow-y-auto max-h-[60vh]">
            {loading ? (
              <div className="text-center">Loading...</div>
            ) : (
              <>
                {activeTab === 'users' && (
                  <UserManagement 
                    users={users} 
                    onCreateUser={createUser} 
                    onDeleteUser={deleteUser} 
                    onUpdateUser={updateUser}
                    groups={groups}
                    editingItem={editingItem}
                    setEditingItem={setEditingItem}
                    editForm={editForm}
                    setEditForm={setEditForm}
                  />
                )}
                {activeTab === 'groups' && (
                  <GroupManagement 
                    groups={groups} 
                    onCreateGroup={createGroup} 
                    onDeleteGroup={deleteGroup}
                    onUpdateGroup={updateGroup}
                    editingItem={editingItem}
                    setEditingItem={setEditingItem}
                    editForm={editForm}
                    setEditForm={setEditForm}
                  />
                )}
                {activeTab === 'documents' && (
                  <DocumentManagement 
                    documents={documents} 
                    groups={groups}
                    users={users}
                    editingItem={editingItem}
                    setEditingItem={setEditingItem}
                    editForm={editForm}
                    setEditForm={setEditForm}
                    onUpdateDocument={loadAllData}
                  />
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function UserManagement({ users, onCreateUser, onDeleteUser, onUpdateUser, groups, editingItem, setEditingItem, editForm, setEditForm }) {
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({ username: '', email: '', password: '', role: 'USER', groupIds: [] });

  const handleSubmit = (e) => {
    e.preventDefault();
    onCreateUser(formData);
    setFormData({ username: '', email: '', password: '', role: 'USER', groupIds: [] });
    setShowForm(false);
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">User Management</h2>
        <button
          onClick={() => setShowForm(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
        >
          Add User
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="mb-6 p-4 border rounded">
          <div className="grid grid-cols-2 gap-4">
            <input
              type="text"
              placeholder="Username"
              value={formData.username}
              onChange={(e) => setFormData({...formData, username: e.target.value})}
              className="border rounded px-3 py-2"
              required
            />
            <input
              type="email"
              placeholder="Email"
              value={formData.email}
              onChange={(e) => setFormData({...formData, email: e.target.value})}
              className="border rounded px-3 py-2"
              required
            />
            <input
              type="password"
              placeholder="Password"
              value={formData.password}
              onChange={(e) => setFormData({...formData, password: e.target.value})}
              className="border rounded px-3 py-2"
              required
            />
            <select
              value={formData.role}
              onChange={(e) => setFormData({...formData, role: e.target.value})}
              className="border rounded px-3 py-2"
            >
              <option value="USER">User</option>
              <option value="ADMIN">Admin</option>
            </select>
            <div className="col-span-2">
              <label className="block text-sm font-medium mb-2">Groups</label>
              <select
                multiple
                value={formData.groupIds || []}
                onChange={(e) => {
                  const selected = Array.from(e.target.selectedOptions, option => option.value);
                  setFormData({...formData, groupIds: selected});
                }}
                className="border rounded px-3 py-2 w-full h-24"
              >
                {groups.map(group => (
                  <option key={group.id} value={group.id}>{group.name}</option>
                ))}
              </select>
              <p className="text-xs text-gray-500 mt-1">Hold Ctrl/Cmd to select multiple groups</p>
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded">Create</button>
            <button type="button" onClick={() => setShowForm(false)} className="bg-gray-600 text-white px-4 py-2 rounded">Cancel</button>
          </div>
        </form>
      )}

      {editingItem && (
        <div className="mb-6 p-4 border rounded bg-yellow-50">
          <h3 className="font-medium mb-4">Edit User</h3>
          <div className="grid grid-cols-2 gap-4">
            <input
              type="text"
              placeholder="Username"
              value={editForm.username || ''}
              onChange={(e) => setEditForm({...editForm, username: e.target.value})}
              className="border rounded px-3 py-2"
            />
            <input
              type="email"
              placeholder="Email"
              value={editForm.email || ''}
              onChange={(e) => setEditForm({...editForm, email: e.target.value})}
              className="border rounded px-3 py-2"
            />
            <select
              value={editForm.role || 'USER'}
              onChange={(e) => setEditForm({...editForm, role: e.target.value})}
              className="border rounded px-3 py-2"
            >
              <option value="USER">User</option>
              <option value="ADMIN">Admin</option>
            </select>
            <div className="col-span-2">
              <label className="block text-sm font-medium mb-2">Groups</label>
              <select
                multiple
                value={editForm.groupIds || []}
                onChange={(e) => {
                  const selected = Array.from(e.target.selectedOptions, option => option.value);
                  setEditForm({...editForm, groupIds: selected});
                }}
                className="border rounded px-3 py-2 w-full h-24"
              >
                {groups.map(group => (
                  <option key={group.id} value={group.id}>{group.name}</option>
                ))}
              </select>
              <p className="text-xs text-gray-500 mt-1">Hold Ctrl/Cmd to select multiple groups</p>
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            <button 
              onClick={() => onUpdateUser(editingItem, editForm)}
              className="bg-blue-600 text-white px-4 py-2 rounded"
            >
              Update
            </button>
            <button 
              onClick={() => setEditingItem(null)}
              className="bg-gray-600 text-white px-4 py-2 rounded"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Username</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Groups</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {users.map((user) => (
              <tr key={user.id}>
                <td className="px-6 py-4 whitespace-nowrap">{user.username}</td>
                <td className="px-6 py-4 whitespace-nowrap">{user.email}</td>
                <td className="px-6 py-4 whitespace-nowrap">{user.role}</td>
                <td className="px-6 py-4 whitespace-nowrap">{user.groupIds?.length || 0} groups</td>
                <td className="px-6 py-4 whitespace-nowrap space-x-2">
                  <button
                    onClick={() => {
                      setEditingItem(user.id);
                      setEditForm({...user});
                    }}
                    className="text-blue-600 hover:text-blue-900"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => onDeleteUser(user.id)}
                    className="text-red-600 hover:text-red-900"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function GroupManagement({ groups, onCreateGroup, onDeleteGroup, onUpdateGroup, editingItem, setEditingItem, editForm, setEditForm }) {
  const [documents, setDocuments] = useState([]);

  useEffect(() => {
    const loadDocuments = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/api/admin/documents`);
        setDocuments(response.data);
      } catch (error) {
        console.error('Error loading documents:', error);
      }
    };
    loadDocuments();
  }, []);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({ name: '', description: '' });

  const handleSubmit = (e) => {
    e.preventDefault();
    onCreateGroup(formData);
    setFormData({ name: '', description: '' });
    setShowForm(false);
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">Group Management</h2>
        <button
          onClick={() => setShowForm(true)}
          className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
        >
          Add Group
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit} className="mb-6 p-4 border rounded">
          <div className="grid grid-cols-2 gap-4">
            <input
              type="text"
              placeholder="Group Name"
              value={formData.name}
              onChange={(e) => setFormData({...formData, name: e.target.value})}
              className="border rounded px-3 py-2"
              required
            />
            <input
              type="text"
              placeholder="Description"
              value={formData.description}
              onChange={(e) => setFormData({...formData, description: e.target.value})}
              className="border rounded px-3 py-2"
            />
          </div>
          <div className="mt-4 flex gap-2">
            <button type="submit" className="bg-green-600 text-white px-4 py-2 rounded">Create</button>
            <button type="button" onClick={() => setShowForm(false)} className="bg-gray-600 text-white px-4 py-2 rounded">Cancel</button>
          </div>
        </form>
      )}

      {editingItem && (
        <div className="mb-6 p-4 border rounded bg-yellow-50">
          <h3 className="font-medium mb-4">Edit Group</h3>
          <div className="grid grid-cols-2 gap-4">
            <input
              type="text"
              placeholder="Group Name"
              value={editForm.name || ''}
              onChange={(e) => setEditForm({...editForm, name: e.target.value})}
              className="border rounded px-3 py-2"
            />
            <input
              type="text"
              placeholder="Description"
              value={editForm.description || ''}
              onChange={(e) => setEditForm({...editForm, description: e.target.value})}
              className="border rounded px-3 py-2"
            />
            <div className="col-span-2">
              <label className="block text-sm font-medium mb-2">Documents</label>
              <select
                multiple
                value={editForm.documentIds || []}
                onChange={(e) => {
                  const selected = Array.from(e.target.selectedOptions, option => option.value);
                  setEditForm({...editForm, documentIds: selected});
                }}
                className="border rounded px-3 py-2 w-full h-24"
              >
                {documents.map(doc => (
                  <option key={doc.id} value={doc.id}>{doc.filename}</option>
                ))}
              </select>
              <p className="text-xs text-gray-500 mt-1">Hold Ctrl/Cmd to select multiple documents</p>
            </div>
          </div>
          <div className="mt-4 flex gap-2">
            <button 
              onClick={() => onUpdateGroup(editingItem, editForm)}
              className="bg-blue-600 text-white px-4 py-2 rounded"
            >
              Update
            </button>
            <button 
              onClick={() => setEditingItem(null)}
              className="bg-gray-600 text-white px-4 py-2 rounded"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Documents</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {groups.map((group) => (
              <tr key={group.id}>
                <td className="px-6 py-4 whitespace-nowrap">{group.name}</td>
                <td className="px-6 py-4 whitespace-nowrap">{group.description}</td>
                <td className="px-6 py-4 whitespace-nowrap">{group.documentIds?.length || 0} documents</td>
                <td className="px-6 py-4 whitespace-nowrap space-x-2">
                  <button
                    onClick={() => {
                      setEditingItem(group.id);
                      setEditForm({...group});
                    }}
                    className="text-blue-600 hover:text-blue-900"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => onDeleteGroup(group.id)}
                    className="text-red-600 hover:text-red-900"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function DocumentManagement({ documents, groups, users, editingItem, setEditingItem, editForm, setEditForm, onUpdateDocument }) {

  const assignDocumentToGroups = async (docId, groupIds) => {
    try {
      await axios.post(`${API_BASE_URL}/api/admin/documents/${docId}/assign-groups`, { groupIds });
      onUpdateDocument();
      setEditingItem(null);
    } catch (error) {
      console.error('Error assigning document to groups:', error);
    }
  };
  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Document Management</h2>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Filename</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Upload Date</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Owner</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {documents.map((doc) => (
              <tr key={doc.id}>
                <td className="px-6 py-4 whitespace-nowrap">{doc.filename}</td>
                <td className="px-6 py-4 whitespace-nowrap">{doc.contentType}</td>
                <td className="px-6 py-4 whitespace-nowrap">{new Date(doc.uploadDate).toLocaleDateString()}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 text-xs rounded ${doc.processed ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}`}>
                    {doc.processed ? 'Processed' : 'Processing'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">{doc.userId}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <button
                    onClick={() => {
                      setEditingItem(doc.id);
                      setEditForm({...doc, selectedGroups: []});
                    }}
                    className="text-blue-600 hover:text-blue-900"
                  >
                    Manage Groups
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editingItem && (
        <div className="mt-6 p-4 border rounded bg-yellow-50">
          <h3 className="font-medium mb-4">Assign Document to Groups</h3>
          <div className="mb-4">
            <p className="text-sm text-gray-600 mb-2">Document: <strong>{editForm.filename}</strong></p>
            <label className="block text-sm font-medium mb-2">Select Groups</label>
            <select
              multiple
              value={editForm.selectedGroups || []}
              onChange={(e) => {
                const selected = Array.from(e.target.selectedOptions, option => option.value);
                setEditForm({...editForm, selectedGroups: selected});
              }}
              className="border rounded px-3 py-2 w-full h-32"
            >
              {groups.map(group => (
                <option key={group.id} value={group.id}>{group.name}</option>
              ))}
            </select>
            <p className="text-xs text-gray-500 mt-1">Hold Ctrl/Cmd to select multiple groups</p>
          </div>
          <div className="flex gap-2">
            <button 
              onClick={() => assignDocumentToGroups(editingItem, editForm.selectedGroups || [])}
              className="bg-blue-600 text-white px-4 py-2 rounded"
            >
              Assign to Groups
            </button>
            <button 
              onClick={() => setEditingItem(null)}
              className="bg-gray-600 text-white px-4 py-2 rounded"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminPanel;