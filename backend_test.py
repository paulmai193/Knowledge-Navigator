import requests
import sys
import os
import time
from datetime import datetime
import tempfile

class KnowledgeNavigatorAPITester:
    def __init__(self, base_url="https://knowledgenav.preview.emergentagent.com"):
        self.base_url = base_url
        self.tests_run = 0
        self.tests_passed = 0
        self.uploaded_document_id = None

    def run_test(self, name, method, endpoint, expected_status, data=None, files=None):
        """Run a single API test"""
        url = f"{self.base_url}/{endpoint}"
        headers = {}
        
        # Don't set Content-Type for file uploads, let requests handle it
        if not files:
            headers['Content-Type'] = 'application/json'

        self.tests_run += 1
        print(f"\n🔍 Testing {name}...")
        print(f"   URL: {url}")
        
        try:
            if method == 'GET':
                response = requests.get(url, headers=headers, timeout=30)
            elif method == 'POST':
                if files:
                    response = requests.post(url, files=files, data=data, timeout=60)
                else:
                    response = requests.post(url, json=data, headers=headers, timeout=30)

            success = response.status_code == expected_status
            if success:
                self.tests_passed += 1
                print(f"✅ Passed - Status: {response.status_code}")
                try:
                    response_data = response.json()
                    print(f"   Response: {response_data}")
                    return True, response_data
                except:
                    print(f"   Response: {response.text[:200]}...")
                    return True, {}
            else:
                print(f"❌ Failed - Expected {expected_status}, got {response.status_code}")
                print(f"   Response: {response.text[:200]}...")
                return False, {}

        except Exception as e:
            print(f"❌ Failed - Error: {str(e)}")
            return False, {}

    def test_health_check(self):
        """Test health check endpoint"""
        success, response = self.run_test(
            "Health Check",
            "GET",
            "api/health",
            200
        )
        return success

    def test_dashboard_stats(self):
        """Test dashboard stats endpoint"""
        success, response = self.run_test(
            "Dashboard Stats",
            "GET",
            "api/dashboard/stats",
            200
        )
        if success:
            expected_keys = ['total_documents', 'processed_documents', 'total_insights', 'total_projects', 'processing_rate']
            for key in expected_keys:
                if key not in response:
                    print(f"⚠️  Warning: Missing key '{key}' in dashboard stats")
        return success

    def test_get_documents_empty(self):
        """Test getting documents when none exist"""
        success, response = self.run_test(
            "Get Documents (Empty)",
            "GET",
            "api/documents",
            200
        )
        return success

    def test_get_insights_empty(self):
        """Test getting insights when none exist"""
        success, response = self.run_test(
            "Get Insights (Empty)",
            "GET",
            "api/insights",
            200
        )
        return success

    def test_get_projects_empty(self):
        """Test getting projects when none exist"""
        success, response = self.run_test(
            "Get Projects (Empty)",
            "GET",
            "api/projects",
            200
        )
        return success

    def create_test_file(self, content, filename, content_type):
        """Create a temporary test file"""
        temp_file = tempfile.NamedTemporaryFile(mode='w', suffix=filename, delete=False)
        temp_file.write(content)
        temp_file.close()
        return temp_file.name

    def test_document_upload_txt(self):
        """Test uploading a text document"""
        test_content = """
        Knowledge Navigator Test Document
        
        This is a test document for the Knowledge Navigator AI system.
        
        Key Points:
        1. This system processes documents using AI
        2. It extracts insights and recommendations
        3. Documents can be organized into projects
        
        Technical Details:
        - Uses Gemini-2.0-flash model
        - Supports PDF, DOCX, and TXT files
        - Provides categorized insights
        
        Best Practices:
        - Upload relevant documents for better insights
        - Use descriptive filenames
        - Organize documents into projects for better management
        
        Recommendations:
        - Regular document processing improves knowledge base
        - Review insights for actionable items
        - Create projects to group related documents
        """
        
        temp_file_path = self.create_test_file(test_content, ".txt", "text/plain")
        
        try:
            with open(temp_file_path, 'rb') as f:
                files = {'file': ('test_document.txt', f, 'text/plain')}
                success, response = self.run_test(
                    "Upload Text Document",
                    "POST",
                    "api/documents/upload",
                    200,
                    files=files
                )
                
                if success and 'document_id' in response:
                    self.uploaded_document_id = response['document_id']
                    print(f"   Document ID: {self.uploaded_document_id}")
                
                return success
        finally:
            os.unlink(temp_file_path)

    def test_document_upload_invalid_type(self):
        """Test uploading an invalid file type"""
        test_content = "This is a test file with invalid extension"
        temp_file_path = self.create_test_file(test_content, ".xyz", "application/octet-stream")
        
        try:
            with open(temp_file_path, 'rb') as f:
                files = {'file': ('test_invalid.xyz', f, 'application/octet-stream')}
                success, response = self.run_test(
                    "Upload Invalid File Type",
                    "POST",
                    "api/documents/upload",
                    400,  # Should return 400 for invalid file type
                    files=files
                )
                return success
        finally:
            os.unlink(temp_file_path)

    def test_get_documents_after_upload(self):
        """Test getting documents after upload"""
        success, response = self.run_test(
            "Get Documents (After Upload)",
            "GET",
            "api/documents",
            200
        )
        
        if success and 'documents' in response:
            documents = response['documents']
            if len(documents) > 0:
                print(f"   Found {len(documents)} document(s)")
                for doc in documents:
                    print(f"   - {doc.get('filename', 'Unknown')} (ID: {doc.get('id', 'Unknown')})")
            else:
                print("   No documents found")
        
        return success

    def test_create_project(self):
        """Test creating a new project"""
        project_data = {
            "name": "Test Project",
            "description": "A test project for the Knowledge Navigator system"
        }
        
        success, response = self.run_test(
            "Create Project",
            "POST",
            "api/projects",
            200,
            data=project_data
        )
        return success

    def test_get_projects_after_creation(self):
        """Test getting projects after creation"""
        success, response = self.run_test(
            "Get Projects (After Creation)",
            "GET",
            "api/projects",
            200
        )
        
        if success and 'projects' in response:
            projects = response['projects']
            if len(projects) > 0:
                print(f"   Found {len(projects)} project(s)")
                for project in projects:
                    print(f"   - {project.get('name', 'Unknown')} (ID: {project.get('id', 'Unknown')})")
            else:
                print("   No projects found")
        
        return success

    def wait_for_document_processing(self, max_wait_time=60):
        """Wait for document processing to complete"""
        print(f"\n⏳ Waiting for document processing (max {max_wait_time}s)...")
        start_time = time.time()
        
        while time.time() - start_time < max_wait_time:
            success, response = self.run_test(
                "Check Document Processing",
                "GET",
                "api/documents",
                200
            )
            
            if success and 'documents' in response:
                documents = response['documents']
                processed_docs = [doc for doc in documents if doc.get('processed', False)]
                
                if len(processed_docs) > 0:
                    print(f"✅ Document processing completed! {len(processed_docs)} document(s) processed")
                    return True
                else:
                    print(f"   Still processing... ({int(time.time() - start_time)}s elapsed)")
                    time.sleep(5)
            else:
                print("   Error checking document status")
                time.sleep(5)
        
        print(f"⚠️  Document processing timeout after {max_wait_time}s")
        return False

    def test_get_insights_after_processing(self):
        """Test getting insights after document processing"""
        success, response = self.run_test(
            "Get Insights (After Processing)",
            "GET",
            "api/insights",
            200
        )
        
        if success and 'insights' in response:
            insights = response['insights']
            if len(insights) > 0:
                print(f"   Found {len(insights)} insight(s)")
                for insight in insights:
                    print(f"   - {insight.get('title', 'Unknown')} ({insight.get('category', 'Unknown')})")
            else:
                print("   No insights found")
        
        return success

    def test_dashboard_stats_after_processing(self):
        """Test dashboard stats after processing"""
        success, response = self.run_test(
            "Dashboard Stats (After Processing)",
            "GET",
            "api/dashboard/stats",
            200
        )
        
        if success:
            print(f"   Total Documents: {response.get('total_documents', 0)}")
            print(f"   Processed Documents: {response.get('processed_documents', 0)}")
            print(f"   Total Insights: {response.get('total_insights', 0)}")
            print(f"   Total Projects: {response.get('total_projects', 0)}")
            print(f"   Total Q&A Sessions: {response.get('total_qa_sessions', 0)}")
            print(f"   Processing Rate: {response.get('processing_rate', 0):.1f}%")
        
        return success

    def test_qa_history_empty(self):
        """Test getting Q&A history when none exist"""
        success, response = self.run_test(
            "Get Q&A History (Empty)",
            "GET",
            "api/qa/history",
            200
        )
        
        if success and 'qa_history' in response:
            qa_history = response['qa_history']
            print(f"   Found {len(qa_history)} Q&A session(s)")
        
        return success

    def test_ask_question_no_documents(self):
        """Test asking a question when no documents exist"""
        question_data = {
            "question": "What are the key insights from the documents?"
        }
        
        success, response = self.run_test(
            "Ask Question (No Documents)",
            "POST",
            "api/qa/ask",
            200,
            data=question_data
        )
        
        if success:
            print(f"   Question: {response.get('question', 'Unknown')}")
            print(f"   Answer: {response.get('answer', 'Unknown')[:100]}...")
            print(f"   Referenced Documents: {len(response.get('referenced_documents', []))}")
            print(f"   Referenced Insights: {len(response.get('referenced_insights', []))}")
        
        return success

    def test_ask_question_empty(self):
        """Test asking an empty question"""
        question_data = {
            "question": ""
        }
        
        success, response = self.run_test(
            "Ask Empty Question",
            "POST",
            "api/qa/ask",
            400,  # Should return 400 for empty question
            data=question_data
        )
        return success

    def test_ask_question_with_documents(self):
        """Test asking a question when documents exist"""
        test_questions = [
            "What are the key technical insights from the uploaded documents?",
            "What are the best practices mentioned in the documents?",
            "Summarize the key technical recommendations",
            "What are the main project insights?",
            "How should I implement UI integration testing?"
        ]
        
        all_success = True
        
        for i, question in enumerate(test_questions):
            question_data = {
                "question": question
            }
            
            success, response = self.run_test(
                f"Ask Question {i+1}: '{question[:50]}...'",
                "POST",
                "api/qa/ask",
                200,
                data=question_data
            )
            
            if success:
                print(f"   Question: {response.get('question', 'Unknown')}")
                print(f"   Answer: {response.get('answer', 'Unknown')[:150]}...")
                print(f"   Referenced Documents: {len(response.get('referenced_documents', []))}")
                print(f"   Referenced Insights: {len(response.get('referenced_insights', []))}")
                
                # Wait a bit between questions to avoid overwhelming the AI
                time.sleep(2)
            else:
                all_success = False
        
        return all_success

    def test_qa_history_after_questions(self):
        """Test getting Q&A history after asking questions"""
        success, response = self.run_test(
            "Get Q&A History (After Questions)",
            "GET",
            "api/qa/history",
            200
        )
        
        if success and 'qa_history' in response:
            qa_history = response['qa_history']
            print(f"   Found {len(qa_history)} Q&A session(s)")
            
            for i, qa in enumerate(qa_history[:3]):  # Show first 3
                print(f"   Q{i+1}: {qa.get('question', 'Unknown')[:50]}...")
                print(f"   A{i+1}: {qa.get('answer', 'Unknown')[:50]}...")
        
        return success

def main():
    print("🚀 Starting Knowledge Navigator API Tests")
    print("=" * 50)
    
    tester = KnowledgeNavigatorAPITester()
    
    # Test sequence
    test_sequence = [
        ("Health Check", tester.test_health_check),
        ("Dashboard Stats (Initial)", tester.test_dashboard_stats),
        ("Get Documents (Empty)", tester.test_get_documents_empty),
        ("Get Insights (Empty)", tester.test_get_insights_empty),
        ("Get Projects (Empty)", tester.test_get_projects_empty),
        ("Upload Text Document", tester.test_document_upload_txt),
        ("Upload Invalid File Type", tester.test_document_upload_invalid_type),
        ("Get Documents (After Upload)", tester.test_get_documents_after_upload),
        ("Create Project", tester.test_create_project),
        ("Get Projects (After Creation)", tester.test_get_projects_after_creation),
    ]
    
    # Run initial tests
    for test_name, test_func in test_sequence:
        try:
            test_func()
        except Exception as e:
            print(f"❌ Test '{test_name}' failed with exception: {str(e)}")
    
    # Wait for document processing
    if tester.uploaded_document_id:
        processing_complete = tester.wait_for_document_processing()
        
        if processing_complete:
            # Test insights after processing
            try:
                tester.test_get_insights_after_processing()
                tester.test_dashboard_stats_after_processing()
            except Exception as e:
                print(f"❌ Post-processing tests failed: {str(e)}")
    
    # Print final results
    print("\n" + "=" * 50)
    print(f"📊 Test Results: {tester.tests_passed}/{tester.tests_run} tests passed")
    
    if tester.tests_passed == tester.tests_run:
        print("🎉 All tests passed!")
        return 0
    else:
        print(f"⚠️  {tester.tests_run - tester.tests_passed} test(s) failed")
        return 1

if __name__ == "__main__":
    sys.exit(main())